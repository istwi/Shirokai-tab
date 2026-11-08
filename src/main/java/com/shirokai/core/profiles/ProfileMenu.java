package com.shirokai.core.profiles;

import com.shirokai.core.titles.*;
import com.shirokai.core.presentation.Surface;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Read-only native container; clicks are actions, never inventory transfers. */
public final class ProfileMenu extends GenericContainerScreenHandler {
    private final ServerPlayerEntity owner;
    private final TitleService titles;
    private final SimpleInventory display;
    private TitleState state;
    private List<TitleDefinition> entries = List.of();
    private int page;
    private boolean busy;
    private boolean closed;
    public ProfileMenu(int syncId, PlayerInventory inventory, ServerPlayerEntity owner, TitleService titles, TitleState state) {
        this(syncId, inventory, owner, titles, state, new SimpleInventory(54));
    }
    private ProfileMenu(int syncId, PlayerInventory inventory, ServerPlayerEntity owner, TitleService titles, TitleState state, SimpleInventory display) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, inventory, display, 6);
        this.owner = owner; this.titles = titles; this.state = state; this.display = display; render();
    }
    public static void open(ServerPlayerEntity player, TitleService titles) {
        player.sendMessage(Text.literal("Carregando perfil…"), false);
        var previous = player.currentScreenHandler;
        titles.view(player.getUuid()).whenCompleteAsync((state, error) -> {
            if (player.isDisconnected() || player.currentScreenHandler != previous) return;
            if (error != null) { player.sendMessage(Text.literal("Não foi possível carregar o perfil; tente novamente."), false); return; }
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inventory, ignored) ->
                    new ProfileMenu(syncId, inventory, player, titles, state), titles.present(player.getUuid(),Surface.MENU_TITLE,"✦ PERFIL SHIROKAI")));
        }, player.getServer());
    }
    private ItemStack icon(Item item, String name, String... lore) {
        var stack = new ItemStack(item);
        stack.set(DataComponentTypes.CUSTOM_NAME, titles.present(owner.getUuid(),Surface.MENU_NAME,name).copy().formatted(Formatting.AQUA));
        stack.set(DataComponentTypes.LORE, new LoreComponent(Arrays.stream(lore).map(line -> (Text) titles.present(owner.getUuid(),Surface.MENU_LORE,line).copy().formatted(Formatting.GRAY)).toList()));
        return stack;
    }
    private void render() {
        display.clear();
        display.setStack(4, icon(Items.PLAYER_HEAD, owner.getGameProfile().getName(),
                "Cargo fixo: " + titles.rank(owner.getUuid()).map(TitleDefinition::name).orElse("Nenhum"),
                "Clã: " + titles.clanName(owner.getUuid()).orElse("Nenhum"),
                "Tags opcionais: " + state.chatIds().size(), "Clique: equipar/remover no chat", "O cargo é definido pelo servidor."));
        entries = titles.catalog(owner.getUuid()).definitions().values().stream()
                .filter(t -> t.category() != TitleDefinition.Category.ROLE && t.chat())
                .sorted(Comparator.comparing(TitleDefinition::category).thenComparing(Comparator.comparingInt(TitleDefinition::priority).reversed()).thenComparing(TitleDefinition::id)).toList();
        var activeChat=titles.chatDefinitions(owner.getUuid()).stream().map(TitleDefinition::id).collect(java.util.stream.Collectors.toSet());
        page = Math.min(page, Math.max(0, (entries.size() - 1) / 36));
        for (int i = 0; i < 36 && page * 36 + i < entries.size(); i++) {
            var title = entries.get(page * 36 + i);
            var availability = titles.rules().availability(title, state, titles.now());
            var stack=icon(availability == TitleRules.Availability.UNLOCKED ? Items.NAME_TAG : Items.BARRIER,
                    title.display(), title.description(), "Estado: " + switch(availability){case UNLOCKED->"Disponível";case LOCKED->"Bloqueado";case EXPIRED->"Expirado";case UNAVAILABLE->"Indisponível";},
                    "Categoria: " + switch(title.category()){case ROLE->"Cargo";case SPECIAL->"Especial";case CLAN->"Clã";}, "Exclusiva do chat",
                    "Chat: " + (state.chatIds().contains(title.id())?(activeChat.contains(title.id())?"Equipado":"Salvo (fora do limite atual)"):"Não equipado"));
            if(titles.menuPresentationEnabled())stack.set(DataComponentTypes.CUSTOM_NAME,titles.display(owner.getUuid(),title).copy().formatted(Formatting.AQUA));
            display.setStack(9+i,stack);
        }
        display.setStack(45, icon(Items.ARROW, "Página anterior"));
        if(titles.customizationEnabled())display.setStack(47,icon(Items.PAINTING,"Estilo dos títulos","Escolha as cores dos títulos personalizáveis.","Cargos com cores fixas mantêm o estilo do servidor."));
        display.setStack(48, icon(Items.MILK_BUCKET, "Remover todas as seleções"));
        display.setStack(49, icon(Items.CLOCK, "Atualizar perfil", "Página " + (page + 1)));
        var back = new ItemStack(Items.ARROW);
        back.set(DataComponentTypes.CUSTOM_NAME, Text.literal("VOLTAR AO MENU").formatted(Formatting.YELLOW, Formatting.BOLD));
        back.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("Retornar ao menu principal.").formatted(Formatting.GRAY), Text.empty(),
                Text.literal("➜ Clique para voltar!").formatted(Formatting.YELLOW))));
        display.setStack(50, back);
        display.setStack(53, icon(Items.ARROW, "Próxima página"));
        sendContentUpdates();
    }
    @Override public boolean canUse(PlayerEntity player) { return !closed && player == owner && !owner.isDisconnected(); }
    @Override public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
    @Override public void onSlotClick(int slot, int button, SlotActionType action, PlayerEntity player) {
        // Never call super: swap/throw/clone/quick-craft/shift-click cannot extract menu icons.
        if (!canUse(player) || owner.currentScreenHandler != this || busy || action != SlotActionType.PICKUP || (button != 0 && button != 1)) return;
        if (slot == 45) { page = Math.max(0, page - 1); render(); return; }
        if (slot == 53) { if ((page + 1) * 36 < entries.size()) page++; render(); return; }
        if (slot == 50) {
            owner.closeHandledScreen();
            var server = owner.getServer();
            if (server.getCommandManager().getDispatcher().getRoot().getChild("menu") != null)
                server.getCommandManager().executeWithPrefix(owner.getCommandSource(), "menu");
            else owner.sendMessage(Text.literal("Menu principal não está disponível neste servidor."), false);
            return;
        }
        if(slot==47&&titles.customizationEnabled()){titles.openCustomization(owner);return;}
        CompletableFuture<TitleState> operation;
        try {
            if (slot == 48) operation = titles.clear(owner.getUuid());
            else if (slot == 49) operation = titles.view(owner.getUuid());
            else if (slot >= 9 && slot < 45 && page * 36 + slot - 9 < entries.size())
                operation = titles.equip(owner.getUuid(), entries.get(page * 36 + slot - 9).id(), true);
            else return;
        } catch (Exception error) { failure(error); return; }
        busy = true;
        operation.whenCompleteAsync((value, error) -> {
            busy = false;
            if (closed || owner.currentScreenHandler != this) return;
            if (error != null) failure(error);
            else { state = value; render(); owner.sendMessage(Text.literal("Perfil atualizado."), false); }
        }, owner.getServer());
    }
    private void failure(Throwable error) {
        while (error.getCause() != null) error = error.getCause();
        owner.sendMessage(Text.literal("Perfil: " + error.getMessage()).formatted(Formatting.RED), false);
    }
    @Override public void onClosed(PlayerEntity player) { closed = true; super.onClosed(player); }
}
