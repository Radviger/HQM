package hqm.client.gui;

import hqm.HQM;
import hqm.client.gui.page.MainPage;
import hqm.net.HQMPacket;
import hqm.net.NetActions;
import hqm.net.Networker;
import hqm.quest.Questbook;
import hqm.quest.SaveHandler;
import hqm.quest.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author canitzp
 */
public class GuiQuestBook extends GuiScreen {

    public static final ResourceLocation LOC_PAGE = new ResourceLocation(HQM.MODID, "textures/gui/book.png");
    public static final ResourceLocation QUESTMAP = new ResourceLocation(HQM.MODID, "textures/gui/questmap.png");
    public static final int PAGE_START_X = 20, PAGE_START_Y = 20, PAGE_WIDTH = 140, PAGE_HEIGHT = 190, PAGE_SECOND_OFFSET = 20;

    private final UUID questbookId;
    public int guiLeft, guiTop, xSize = 340, ySize = 234;
    private int lastX, lastY;
    private ArrayDeque<IPage> lastPages = new ArrayDeque<>();
    private IPage currentPage, rewindButtonPage;
    private List<IRenderer> renderer = new CopyOnWriteArrayList<>();
    private Team team;
    private ItemStack book;
    private EntityPlayer player;

    public GuiQuestBook(UUID questbookId, EntityPlayer player, ItemStack book){
        this.questbookId = questbookId;
        this.team = this.getQuestbook().getTeam(player);
        this.book = book;
        this.player = player;
    }

    public Questbook getQuestbook(){
        Questbook questbook = SaveHandler.QUEST_DATA.get(this.questbookId);
        if(questbook == null){
            Minecraft.getMinecraft().displayGuiScreen(null);
        }
        return questbook;
    }

    public Team getTeam() {
        return team;
    }

    public void tryToLoadPage(ItemStack stack) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        if(!stack.isEmpty() && stack.hasTagCompound()){
            String clazz = stack.getTagCompound().getString("PageClass");
            if(!clazz.isEmpty()){
                Class c = Class.forName(clazz);
                if(c != null && IPage.class.isAssignableFrom(c)){
                    this.setPage((IPage) c.newInstance(), false);
                }
            }
        }
    }

    public void setPage(IPage page, boolean addToLastPageIndex){
        if(addToLastPageIndex && this.currentPage != null){
            this.lastPages.addFirst(this.currentPage);
        }
        if(this.rewindButtonPage == page){
            this.lastPages.clear();
        }
        this.currentPage = page;
        this.renderer.clear();
        this.currentPage.init(this);
    }

    public void setRewindPage(IPage page){
        this.rewindButtonPage = page;
    }

    public void addRenderer(IRenderer renderer){
        this.renderer.add(renderer);
    }

    public void bindTexture(ResourceLocation loc){
        this.mc.getTextureManager().bindTexture(loc);
    }

    @Override
    public void initGui() {
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        if(this.currentPage == null){
            this.setPage(new MainPage(), false);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.pushMatrix();
        this.bindTexture(LOC_PAGE);
        this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, xSize / 2, ySize);
        GlStateManager.translate(this.guiLeft + xSize, this.guiTop + ySize, this.zLevel); // nix touchy pls
        GlStateManager.rotate(180, 0, 0, 1); // nix touchy pls
        this.drawTexturedModalRect(0, 0, 0, 0, xSize / 2, ySize); // nix touchy pls
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        this.currentPage.render(this, this.guiLeft + PAGE_START_X, this.guiTop + PAGE_START_Y, mouseX, mouseY, IPage.Side.LEFT);
        this.currentPage.render(this, this.guiLeft + PAGE_START_X + PAGE_SECOND_OFFSET + PAGE_WIDTH, this.guiTop + PAGE_START_Y, mouseX, mouseY, IPage.Side.RIGHT);
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        this.renderer.forEach(renderer1 -> {
            renderer1.draw(this, this.guiLeft + PAGE_START_X, this.guiTop + PAGE_START_Y, PAGE_WIDTH, PAGE_HEIGHT, mouseX, mouseY, IPage.Side.LEFT);
            renderer1.draw(this, this.guiLeft + PAGE_START_X + PAGE_SECOND_OFFSET + PAGE_WIDTH, this.guiTop + PAGE_START_Y, PAGE_WIDTH, PAGE_HEIGHT, mouseX, mouseY, IPage.Side.RIGHT);
        });
        GlStateManager.popMatrix();
        if(!this.lastPages.isEmpty() || this.rewindButtonPage != null){
            GlStateManager.pushMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.bindTexture(QUESTMAP);
            if(!this.lastPages.isEmpty()){
                boolean isMouseOver = mouseX >= guiLeft + 7 && mouseX <= guiLeft + 7 + 15 && mouseY >= guiTop + 219 && mouseY <= guiTop + 219 + 10;
                this.drawTexturedModalRect(guiLeft + 7, guiTop + 219, isMouseOver ? 15 : 0, 113, 15, 10);
            }
            if(this.rewindButtonPage != null && this.rewindButtonPage != this.currentPage){
                boolean isMouseOver = mouseX >= guiLeft + 162 && mouseX <= guiLeft + 162 + 14 && mouseY >= guiTop + 217 && mouseY <= guiTop + 217 + 9;
                this.drawTexturedModalRect(guiLeft + 162, guiTop + 217, isMouseOver ? 14 : 0, 104, 14, 9);
            }
            GlStateManager.popMatrix();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int left = this.guiLeft + PAGE_START_X;
        int top = this.guiTop + PAGE_START_Y;
        if(mouseX >= left && mouseX <= left + PAGE_WIDTH + 20 && mouseY >= top && mouseY <= top + PAGE_HEIGHT + 20){
            int finalLeft = left;
            this.renderer.forEach(renderer1 -> renderer1.mouseClick(this, finalLeft, top, PAGE_WIDTH, PAGE_HEIGHT, mouseX, mouseY, mouseButton, IPage.Side.LEFT));
        }
        left += PAGE_SECOND_OFFSET + PAGE_WIDTH;
        if(mouseX >= left && mouseX <= left + PAGE_WIDTH + 20 && mouseY >= top && mouseY <= top + PAGE_HEIGHT + 20){
            int finalLeft = left;
            this.renderer.forEach(renderer1 -> renderer1.mouseClick(this, finalLeft, top, PAGE_WIDTH, PAGE_HEIGHT, mouseX, mouseY, mouseButton, IPage.Side.RIGHT));
        }
        if(!this.lastPages.isEmpty() && mouseX >= guiLeft + 7 && mouseX <= guiLeft + 7 + 15 && mouseY >= guiTop + 219 && mouseY <= guiTop + 219 + 10){ // back button
            this.setPage(this.lastPages.removeFirst(), false);
        }
        if(this.rewindButtonPage != null && this.rewindButtonPage != this.currentPage && mouseX >= guiLeft + 162 && mouseX <= guiLeft + 162 + 14 && mouseY >= guiTop + 217 && mouseY <= guiTop + 217 + 9){ // rewind button
            this.setPage(this.rewindButtonPage, false);
        }
        this.lastX = mouseX;
        this.lastY = mouseY;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        int left = this.guiLeft + PAGE_START_X;
        int top = this.guiTop + PAGE_START_Y;
        if(mouseX >= left && mouseX <= left + PAGE_WIDTH + 20 && mouseY >= top && mouseY <= top + PAGE_HEIGHT + 20){
            for (IRenderer renderer1 : this.renderer) {
                renderer1.mouseRelease(this, left, top, PAGE_WIDTH, PAGE_HEIGHT, mouseX, mouseY, mouseButton, IPage.Side.LEFT);
            }
        }
        left += PAGE_SECOND_OFFSET + PAGE_WIDTH;
        if(mouseX >= left && mouseX <= left + PAGE_WIDTH + 20 && mouseY >= top && mouseY <= top + PAGE_HEIGHT + 20){
            for (IRenderer renderer1 : this.renderer) {
                renderer1.mouseRelease(this, left, top, PAGE_WIDTH, PAGE_HEIGHT, mouseX, mouseY, mouseButton, IPage.Side.RIGHT);
            }
        }
        this.lastX = 0;
        this.lastY = 0;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int mouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, mouseButton, timeSinceLastClick);
        int left = this.guiLeft + PAGE_START_X;
        int top = this.guiTop + PAGE_START_Y;
        if(mouseX >= left && mouseX <= left + PAGE_WIDTH && mouseY >= top && mouseY <= top + PAGE_HEIGHT){
            int finalLeft = left;
            this.renderer.forEach(renderer1 -> renderer1.mouseClickMove(this, finalLeft, top, PAGE_WIDTH, PAGE_HEIGHT, mouseX, mouseY, lastX, lastY, mouseButton, timeSinceLastClick, IPage.Side.LEFT));
        }
        left += PAGE_SECOND_OFFSET + PAGE_WIDTH;
        if(mouseX >= left && mouseX <= left + PAGE_WIDTH && mouseY >= top && mouseY <= top + PAGE_HEIGHT){
            int finalLeft = left;
            this.renderer.forEach(renderer1 -> renderer1.mouseClickMove(this, finalLeft, top, PAGE_WIDTH, PAGE_HEIGHT, mouseX, mouseY, lastX, lastY, mouseButton, timeSinceLastClick,  IPage.Side.RIGHT));
        }
        this.lastX = mouseX;
        this.lastY = mouseY;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int mouseX = (Mouse.getEventX() * this.width / this.mc.displayWidth);
        int mouseY = (this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1);
        int scroll = Mouse.getEventDWheel();
        if(scroll != 0){
            int left = this.guiLeft + PAGE_START_X;
            int top = this.guiTop + PAGE_START_Y;
            if(mouseX >= left && mouseX <= left + PAGE_WIDTH && mouseY >= top && mouseY <= top + PAGE_HEIGHT){
                int finalLeft = left;
                this.renderer.forEach(renderer1 -> renderer1.mouseScroll(this, finalLeft, top, PAGE_WIDTH, PAGE_HEIGHT, mouseX, mouseY, scroll, IPage.Side.LEFT));
            }
            left += PAGE_SECOND_OFFSET + PAGE_WIDTH;
            if(mouseX >= left && mouseX <= left + PAGE_WIDTH && mouseY >= top && mouseY <= top + PAGE_HEIGHT){
                int finalLeft = left;
                this.renderer.forEach(renderer1 -> renderer1.mouseScroll(this, finalLeft, top, PAGE_WIDTH, PAGE_HEIGHT, mouseX, mouseY, scroll,  IPage.Side.RIGHT));
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setInteger("CurrentSlot", this.player.inventory.getSlotFor(this.book));
        nbt.setTag("Data", Networker.singleTag("PageClass", new NBTTagString(this.currentPage.getClass().getName())));
        Networker.NET.sendToServer(new HQMPacket(NetActions.STACK_ADD_NBT, this.player, nbt));
    }
}
