package com.shirokai.core.customization;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.shirokai.core.CoreRuntime;
import com.shirokai.core.presentation.Surface;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import com.shirokai.core.messages.Messages;
import java.util.function.Supplier;
import static net.minecraft.server.command.CommandManager.*;

public final class StyleChoiceCommands {
    private final Supplier<CoreRuntime> runtime;
    public StyleChoiceCommands(Supplier<CoreRuntime> runtime){this.runtime=runtime;}
    public void register(CommandDispatcher<ServerCommandSource> dispatcher){
        if(dispatcher.getRoot().getChild("estilo")!=null){org.slf4j.LoggerFactory.getLogger("shirokai-core").warn("[styles] /estilo já existe; preservado");return;}
        var root=literal("estilo");
        for(var surface:PresetCatalog.SURFACES){
            String name=switch(surface){case TITLE->"titulo";case CLAN_NAME->"nome";default->"sigla";};
            root.then(literal(name).executes(c->execute(c.getSource(),surface,null)).then(argument("preset",StringArgumentType.word())
                .suggests((c,b)->{
                    var core=runtime.get();var player=c.getSource().getPlayer();
                    b.suggest("padrao");
                    if(core!=null&&player!=null&&core.customization()!=null)core.customization().catalog().presets().keySet().stream()
                        .filter(id->id.startsWith(b.getRemainingLowerCase())&&core.customization().allowed(player.getUuid(),surface,id)).forEach(b::suggest);
                    return b.buildFuture();
                }).executes(c->execute(c.getSource(),surface,StringArgumentType.getString(c,"preset")))));
        }
        dispatcher.register(root);
    }
    private int execute(ServerCommandSource source,Surface surface,String id){
        var core=runtime.get();var player=source.getPlayer();
        if(player==null||core==null||core.customization()==null){source.sendError(Messages.error("Estilos indisponíveis. Entre no servidor e tente novamente."));return 0;}
        var service=core.customization();
        if(id==null){StyleChoiceMenu.open(player,service,surface,null);return 1;}
        try{
            var previous=player.currentScreenHandler;
            service.view(player.getUuid(),surface,null).thenComposeAsync(view->service.choose(player.getUuid(),view,id.equals("padrao")?null:id,
                ()->!player.isDisconnected()&&player.currentScreenHandler==previous),source.getServer()).whenCompleteAsync((choices,error)->{
                if(error==null)source.sendFeedback(()->Messages.info(id.equals("padrao")?"Estilo padrão restaurado.":"Estilo aplicado: "+id+"."),false);
                else {while(error.getCause()!=null)error=error.getCause();source.sendError(Messages.error("Estilos: "+error.getMessage()));}
            },source.getServer());
        }catch(Exception error){source.sendError(Messages.error("Estilos: "+error.getMessage()));return 0;}
        return 1;
    }
}
