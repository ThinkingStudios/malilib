package fi.dy.masa.malilib.gui.action;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import fi.dy.masa.malilib.MaLiLibConfigs;
import fi.dy.masa.malilib.action.ActionContext;
import fi.dy.masa.malilib.action.ActionExecutionWidgetManager;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.TextInputScreen;
import fi.dy.masa.malilib.gui.icon.DefaultIcons;
import fi.dy.masa.malilib.gui.position.EdgeInt;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widget.CheckBoxWidget;
import fi.dy.masa.malilib.gui.widget.InfoIconWidget;
import fi.dy.masa.malilib.gui.widget.IntegerEditWidget;
import fi.dy.masa.malilib.gui.widget.LabelWidget;
import fi.dy.masa.malilib.gui.widget.MenuEntryWidget;
import fi.dy.masa.malilib.gui.widget.MenuWidget;
import fi.dy.masa.malilib.gui.widget.button.BaseButton;
import fi.dy.masa.malilib.gui.widget.button.GenericButton;
import fi.dy.masa.malilib.gui.widget.button.OnOffButton;
import fi.dy.masa.malilib.input.ActionResult;
import fi.dy.masa.malilib.input.KeyBindImpl;
import fi.dy.masa.malilib.overlay.message.MessageUtils;
import fi.dy.masa.malilib.render.ShapeRenderUtils;
import fi.dy.masa.malilib.render.text.StyledText;
import fi.dy.masa.malilib.render.text.StyledTextLine;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.data.Vec2i;

public class ActionWidgetScreen extends BaseScreen implements ActionWidgetContainer
{
    protected final ActionWidgetScreenData data;
    protected final List<BaseActionExecutionWidget> widgetList = new ArrayList<>();
    protected final List<BaseActionExecutionWidget> selectedWidgets = new ArrayList<>();
    protected final GenericButton addWidgetButton;
    protected final GenericButton editModeButton;
    protected final GenericButton gridEnabledButton;
    protected final GenericButton exportSettingsButton;
    protected final GenericButton importSettingsButton;
    protected final LabelWidget editModeLabel;
    protected final LabelWidget gridLabel;
    protected final IntegerEditWidget gridEditWidget;
    protected final CheckBoxWidget closeOnExecuteCheckbox;
    protected final CheckBoxWidget closeOnKeyReleaseCheckbox;
    protected final InfoIconWidget infoWidget;
    protected final String name;
    @Nullable protected MenuWidget menuWidget;
    protected Vec2i selectionStart = Vec2i.ZERO;
    protected boolean closeScreenOnExecute;
    protected boolean closeScreenOnKeyRelease;
    protected boolean dirty;
    protected boolean dragSelecting;
    protected boolean editMode;
    protected boolean gridEnabled = true;
    protected boolean hasGroupSelection;
    protected int gridSize = 8;

    public ActionWidgetScreen(String name, ActionWidgetScreenData data)
    {
        this.data = data;
        this.name = name;

        this.editModeLabel = new LabelWidget(0, 0, 0xFFFFFFFF, "malilib.label.edit_mode.colon");
        this.editModeButton = OnOffButton.simpleSlider(16, () -> this.editMode, this::toggleEditMode);

        this.infoWidget = new InfoIconWidget(0, 0, DefaultIcons.INFO_ICON_18, "");

        this.closeOnExecuteCheckbox = new CheckBoxWidget(0, 0, "malilib.label.checkbox.action_widget_screen.close_on_execute",
                                                         "malilib.hover_info.action_widget_screen.close_screen_on_execute");
        this.closeOnExecuteCheckbox.setBooleanStorage(this::shouldCloseScreenOnExecute, this::setCloseScreenOnExecute);

        this.closeOnKeyReleaseCheckbox = new CheckBoxWidget(0, 0, "malilib.label.checkbox.action_widget_screen.close_on_key_release",
                                                            "malilib.hover_info.action_widget_screen.close_screen_on_key_release");
        this.closeOnKeyReleaseCheckbox.setBooleanStorage(() -> this.closeScreenOnKeyRelease, this::setCloseScreenOnKeyRelease);

        this.addWidgetButton = new GenericButton(0, 0, -1, 16, "malilib.label.button.add_action");
        this.addWidgetButton.setActionListener(this::openAddWidgetScreen);

        this.exportSettingsButton = new GenericButton(0, 0, -1, 16, "malilib.gui.button.export");
        this.exportSettingsButton.translateAndAddHoverString("malilib.gui.button.hover.action_widget_screen.export_settings");
        this.exportSettingsButton.setActionListener(this::onExportSettings);

        this.importSettingsButton = new GenericButton(0, 0, -1, 16, "malilib.gui.button.import");
        this.importSettingsButton.translateAndAddHoverString("malilib.gui.button.hover.action_widget_screen.import_settings");
        this.importSettingsButton.setActionListener(this::onImportSettings);

        this.gridLabel = new LabelWidget(0, 0, 0xFFFFFFFF, "malilib.label.grid.colon");
        this.gridEnabledButton = OnOffButton.simpleSlider(16, () -> this.gridEnabled, this::toggleGridEnabled);
        this.gridEditWidget = new IntegerEditWidget(0, 0, 40, 16, this.gridSize, 1, 256, this::setGridSize);

        this.readData(data);
    }

    @Override
    protected void initScreen()
    {
        super.initScreen();

        int x = this.x + 10;
        int y = this.y + 2;

        this.editModeLabel.setPosition(x, y + 4);
        this.editModeButton.setPosition(this.editModeLabel.getRight() + 6, y);

        this.addWidget(this.editModeLabel);
        this.addWidget(this.editModeButton);

        if (this.editMode)
        {
            this.gridLabel.setPosition(this.editModeButton.getRight() + 16, y + 4);
            this.gridEnabledButton.setPosition(this.gridLabel.getRight() + 6, y);
            this.gridEditWidget.setPosition(this.gridEnabledButton.getRight() + 6, y);
            this.exportSettingsButton.setPosition(this.gridEditWidget.getRight() + 6, y);
            this.importSettingsButton.setPosition(this.exportSettingsButton.getRight() + 6, y);
            y += 20;

            this.closeOnExecuteCheckbox.setPosition(x, y);
            y += 12;
            this.closeOnKeyReleaseCheckbox.setPosition(x, y);

            int tmpX = Math.max(this.closeOnExecuteCheckbox.getRight(), this.closeOnKeyReleaseCheckbox.getRight()) + 6;
            this.infoWidget.setPosition(tmpX, this.closeOnExecuteCheckbox.getY());
            y += 14;

            this.addWidgetButton.setPosition(x, y);

            this.addWidget(this.closeOnExecuteCheckbox);
            this.addWidget(this.closeOnKeyReleaseCheckbox);

            this.addWidget(this.addWidgetButton);
            this.addWidget(this.infoWidget);

            this.addWidget(this.gridLabel);
            this.addWidget(this.gridEnabledButton);

            this.addWidget(this.exportSettingsButton);
            this.addWidget(this.importSettingsButton);

            if (this.gridEnabled)
            {
                this.addWidget(this.gridEditWidget);
            }
        }

        for (BaseActionExecutionWidget widget : this.widgetList)
        {
            widget.setContainer(this);
            widget.onAdded(this);
            this.addWidget(widget);
        }

        this.hasGroupSelection = this.selectedWidgets.isEmpty() == false;
    }

    @Override
    public void onGuiClosed()
    {
        super.onGuiClosed();

        if (this.dirty ||
            this.closeScreenOnExecute != this.data.closeScreenOnExecute ||
            this.closeScreenOnKeyRelease != this.data.closeScreenOnKeyRelease)
        {
            ActionWidgetScreenData data = this.getCurrentData();
            ActionExecutionWidgetManager.INSTANCE.saveWidgetScreenData(this.name, data);
        }

        this.clearActionWidgetContainerData();
    }

    @Override
    public boolean onMouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        if (this.editMode)
        {
            if (mouseButton == 0)
            {
                if (this.menuWidget != null)
                {
                    this.menuWidget.tryMouseClick(mouseX, mouseY, mouseButton);
                    this.closeMenu();

                    return true;
                }

                if (this.hasGroupSelection && this.leftClickWithGroupSelection(mouseX, mouseY))
                {
                    return true;
                }

                if (this.hoveredWidget == null)
                {
                    this.selectionStart = new Vec2i(mouseX, mouseY);
                    this.dragSelecting = true;
                    return true;
                }
            }
            else if (mouseButton == 1 && isShiftDown() == false)
            {
                if (this.hasGroupSelection)
                {
                    this.openGroupMenu(mouseX, mouseY);
                    return true;
                }
                else
                {
                    // Close any possible previous menu
                    this.closeMenu();

                    BaseActionExecutionWidget widget = this.getHoveredActionWidget(mouseX, mouseY);

                    if (widget != null)
                    {
                        this.openSingleWidgetMenu(mouseX, mouseY, widget);
                        return true;
                    }
                }
            }
        }

        return super.onMouseClicked(mouseX, mouseY, mouseButton);
    }

    protected boolean leftClickWithGroupSelection(int mouseX, int mouseY)
    {
        boolean clickedWidget = this.getHoveredActionWidget(mouseX, mouseY) != null;

        if (clickedWidget)
        {
            for (BaseActionExecutionWidget widget : this.selectedWidgets)
            {
                widget.startDragging(mouseX, mouseY);
            }

            return true;
        }
        else if (isCtrlDown() == false && this.hoveredWidget == null)
        {
            this.clearSelectedWidgets();
        }

        return false;
    }

    @Override
    public boolean onMouseReleased(int mouseX, int mouseY, int mouseButton)
    {
        if (this.dragSelecting)
        {
            int minX = Math.min(mouseX, this.selectionStart.x);
            int minY = Math.min(mouseY, this.selectionStart.y);
            int maxX = Math.max(mouseX, this.selectionStart.x);
            int maxY = Math.max(mouseY, this.selectionStart.y);
            EdgeInt selection = new EdgeInt(minY, maxX, maxY, minX);

            for (BaseActionExecutionWidget widget : this.widgetList)
            {
                if (widget.intersects(selection))
                {
                    widget.toggleSelected();
                    this.selectedWidgets.remove(widget);

                    if (widget.isSelected())
                    {
                        this.selectedWidgets.add(widget);
                    }
                }
            }

            this.dragSelecting = false;
            this.hasGroupSelection = this.selectedWidgets.isEmpty() == false;
        }

        return super.onMouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == Keyboard.KEY_DELETE)
        {
            this.deleteSelectedWidgets();
            return true;
        }

        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean onKeyReleased(int keyCode, int scanCode, int modifiers)
    {
        if (this.editMode == false && this.closeScreenOnKeyRelease &&
            KeyBindImpl.getCurrentlyPressedKeysCount() == 0)
        {
            this.closeScreen(false);

            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            BaseActionExecutionWidget widget = this.getHoveredActionWidget(mouseX, mouseY);

            if (widget != null)
            {
                widget.executeAction();
            }
        }

        return false;
    }

    protected void clearActionWidgetContainerData()
    {
        for (BaseActionExecutionWidget widget : this.widgetList)
        {
            // Remove the references to this screen
            widget.setContainer(null);
        }
    }

    protected void readData(ActionWidgetScreenData data)
    {
        this.clearActionWidgetContainerData();

        this.widgetList.clear();
        this.selectedWidgets.clear();

        this.closeScreenOnExecute = data.closeScreenOnExecute;
        this.closeScreenOnKeyRelease = data.closeScreenOnKeyRelease;

        for (BaseActionExecutionWidget widget : data.widgets)
        {
            this.widgetList.add(widget);

            if (widget.isSelected())
            {
                this.selectedWidgets.add(widget);
            }
        }
    }

    @Nullable
    protected BaseActionExecutionWidget getHoveredActionWidget(int mouseX, int mouseY)
    {
        for (BaseActionExecutionWidget widget : this.widgetList)
        {
            if (widget.isMouseOver(mouseX, mouseY))
            {
                return widget;
            }
        }

        return null;
    }

    protected ActionWidgetScreenData getCurrentData()
    {
        ImmutableList<BaseActionExecutionWidget> widgets = ImmutableList.copyOf(this.widgetList);
        return new ActionWidgetScreenData(widgets, this.closeScreenOnExecute, this.closeScreenOnKeyRelease);
    }

    protected void closeMenu()
    {
        this.removeWidget(this.menuWidget);
        this.menuWidget = null;
    }

    protected void openSingleWidgetMenu(int mouseX, int mouseY, BaseActionExecutionWidget widget)
    {
        this.menuWidget = new MenuWidget(mouseX + 4, mouseY, 10, 10);

        StyledTextLine textEdit = StyledTextLine.translate("malilib.label.edit");
        StyledTextLine textRemove = StyledTextLine.translate("malilib.label.delete.colored");
        this.menuWidget.setMenuEntries(new MenuEntryWidget(textEdit, () -> this.editActionWidget(widget)),
                                       new MenuEntryWidget(textRemove, () -> this.removeActionWidget(widget)));

        this.addWidget(this.menuWidget);
        this.menuWidget.setZLevel(this.zLevel + 40);
        this.menuWidget.updateSubWidgetsToGeometryChanges();
    }

    protected void openGroupMenu(int mouseX, int mouseY)
    {
        this.menuWidget = new MenuWidget(mouseX + 4, mouseY, 10, 10);

        StyledTextLine textEdit = StyledTextLine.translate("malilib.label.edit_selected");
        StyledTextLine textRemove = StyledTextLine.translate("malilib.label.delete_selected.colored");
        this.menuWidget.setMenuEntries(new MenuEntryWidget(textEdit, this::editSelectedWidgets),
                                       new MenuEntryWidget(textRemove, this::deleteSelectedWidgets));

        this.addWidget(this.menuWidget);
        this.menuWidget.setZLevel(this.zLevel + 40);
        this.menuWidget.updateSubWidgetsToGeometryChanges();
    }

    protected void addActionWidget(BaseActionExecutionWidget widget)
    {
        widget.setContainer(this);
        widget.setPosition(this.x + this.screenWidth / 2, this.y + 10);
        widget.onAdded(this);

        this.widgetList.add(widget);
        this.addWidget(widget);
        this.notifyWidgetEdited();
    }

    protected void removeActionWidget(BaseActionExecutionWidget widget)
    {
        widget.setContainer(null);
        this.widgetList.remove(widget);
        this.selectedWidgets.remove(widget);
        this.removeWidget(widget);
        this.notifyWidgetEdited();
    }

    protected void editActionWidget(BaseActionExecutionWidget widget)
    {
        this.openActionWidgetEditScreen(ImmutableList.of(widget));
    }

    protected void editSelectedWidgets()
    {
        if (this.selectedWidgets.isEmpty() == false)
        {
            this.openActionWidgetEditScreen(ImmutableList.copyOf(this.selectedWidgets));
        }
    }

    protected void deleteSelectedWidgets()
    {
        if (this.selectedWidgets.isEmpty() == false)
        {
            this.widgetList.removeAll(this.selectedWidgets);

            for (BaseActionExecutionWidget widget : this.selectedWidgets)
            {
                this.removeWidget(widget);
            }

            this.selectedWidgets.clear();
            this.hasGroupSelection = false;
            this.notifyWidgetEdited();
        }
    }

    protected void clearSelectedWidgets()
    {
        for (BaseActionExecutionWidget widget : this.selectedWidgets)
        {
            widget.setSelected(false);
        }

        this.selectedWidgets.clear();
        this.hasGroupSelection = false;
    }

    @Override
    public boolean isEditMode()
    {
        return this.editMode;
    }

    @Override
    public int getGridSize()
    {
        return this.gridEnabled ? this.gridSize : -1;
    }

    @Override
    public boolean shouldCloseScreenOnExecute()
    {
        return this.closeScreenOnExecute;
    }

    @Override
    public void notifyWidgetEdited()
    {
        this.dirty = true;
    }

    protected void toggleGridEnabled()
    {
        this.gridEnabled = ! this.gridEnabled;

        this.removeWidget(this.gridEditWidget);

        if (this.gridEnabled)
        {
            this.addWidget(this.gridEditWidget);
        }
    }

    public void setCloseScreenOnExecute(boolean closeScreenOnExecute)
    {
        this.closeScreenOnExecute = closeScreenOnExecute;
    }

    public void setCloseScreenOnKeyRelease(boolean closeScreenOnKeyRelease)
    {
        this.closeScreenOnKeyRelease = closeScreenOnKeyRelease;
    }

    protected void toggleEditMode()
    {
        this.editMode = ! this.editMode;
        this.initScreen();
    }

    protected void setGridSize(int gridSize)
    {
        this.gridSize = gridSize;
    }

    protected void openAddWidgetScreen()
    {
        AddActionExecutionWidgetScreen screen = new AddActionExecutionWidgetScreen(this::addActionWidget);
        screen.setParent(this);
        BaseScreen.openPopupScreen(screen);
    }

    protected void openActionWidgetEditScreen(List<BaseActionExecutionWidget> widgets)
    {
        EditActionExecutionWidgetScreen screen = new EditActionExecutionWidgetScreen(widgets);
        screen.setParent(this);
        BaseScreen.openPopupScreen(screen);

        // Not technically correct... but micro-optimizing here and detecting
        // widget changes is probably a waste of time
        this.notifyWidgetEdited();
    }

    protected boolean onExportSettings(BaseButton button, int mouseButton)
    {
        if (mouseButton == 1 && isCtrlDown() && isShiftDown())
        {
            setClipboardString(this.getSettingsExportString());
            MessageUtils.success("malilib.message.action_screen_settings_copied_to_clipboard");
        }
        else if (mouseButton == 0)
        {
            String str = this.getSettingsExportString();
            TextInputScreen screen = new TextInputScreen("malilib.gui.title.action_screen.export_settings",
                                                         str, this, (s) -> true);
            openPopupScreen(screen);
        }

        return true;
    }

    protected boolean onImportSettings(BaseButton button, int mouseButton)
    {
        if (mouseButton == 1 && isCtrlDown() && isShiftDown())
        {
            this.applySettingsFromImportString(getClipboardString());
        }
        else if (mouseButton == 0)
        {
            TextInputScreen screen = new TextInputScreen("malilib.gui.title.action_screen.import_settings",
                                                         "", this, this::applySettingsFromImportString);
            openPopupScreen(screen);
        }

        return true;
    }

    protected String getSettingsExportString()
    {
        ActionWidgetScreenData data = this.getCurrentData();
        return JsonUtils.jsonToString(data.toJson(), true);
    }

    protected boolean applySettingsFromImportString(@Nullable String str)
    {
        if (str != null)
        {
            JsonElement el = JsonUtils.parseJsonFromString(str);

            if (el != null)
            {
                ActionWidgetScreenData data = ActionWidgetScreenData.fromJson(el);

                if (data != null)
                {
                    this.readData(data);
                    this.initScreen();
                    this.notifyWidgetEdited();
                    MessageUtils.success("malilib.message.action_screen_settings_imported");
                    return true;
                }
            }
        }
    
        return false;
    }

    @Override
    protected void drawContents(int mouseX, int mouseY, float partialTicks)
    {
        if (this.editMode)
        {
            if (this.gridEnabled)
            {
                int color = 0x30FFFFFF;

                ShapeRenderUtils.renderGrid(this.x, this.y, this.zLevel + 0.1f,
                                            this.screenWidth, this.screenHeight, this.gridSize, 1, color);
            }

            if (this.dragSelecting)
            {
                int minX = Math.min(mouseX, this.selectionStart.x);
                int minY = Math.min(mouseY, this.selectionStart.y);
                int maxX = Math.max(mouseX, this.selectionStart.x);
                int maxY = Math.max(mouseY, this.selectionStart.y);

                ShapeRenderUtils.renderOutlinedRectangle(minX, minY, this.zLevel + 50f,
                                                         maxX - minX, maxY - minY, 0x30FFFFFF, 0xFFFFFFFF);
            }
        }

        super.drawContents(mouseX, mouseY, partialTicks);
    }

    public static void openCreateActionWidgetScreen()
    {
        TextInputScreen screen = new TextInputScreen("malilib.gui.title.create_action_widget_screen", "",
                                                     GuiUtils.getCurrentScreen(),
                                                     ActionExecutionWidgetManager::createActionWidgetScreen);
        screen.setInfoText(StyledText.translate("malilib.gui.info.create_action_widget_screen.name_is_final"));
        screen.setLabelText(StyledText.translate("malilib.label.name.colon"));
        BaseScreen.openPopupScreen(screen);
    }

    public static ActionResult openActionWidgetScreen(ActionContext ctx, String arg)
    {
        ActionWidgetScreenData data = ActionExecutionWidgetManager.INSTANCE.getOrLoadWidgetScreenData(arg);

        if (data != null)
        {
            MaLiLibConfigs.Internal.PREVIOUS_ACTION_WIDGET_SCREEN.setValue(arg);
            ActionWidgetScreen screen = new ActionWidgetScreen(arg, data);
            BaseScreen.openScreen(screen);
            return ActionResult.SUCCESS;
        }
        else
        {
            MessageUtils.error("malilib.message.error.no_action_screen_found_by_name", arg);
            return ActionResult.FAIL;
        }
    }

    public static ActionResult openPreviousActionWidgetScreen(ActionContext ctx)
    {
        String name = MaLiLibConfigs.Internal.PREVIOUS_ACTION_WIDGET_SCREEN.getStringValue();

        if (StringUtils.isBlank(name))
        {
            MessageUtils.error("malilib.message.error.no_previously_opened_action_screen");
            return ActionResult.FAIL;
        }

        return openActionWidgetScreen(ctx, name);
    }
}
