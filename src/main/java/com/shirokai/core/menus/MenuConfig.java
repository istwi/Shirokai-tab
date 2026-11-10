package com.shirokai.core.menus;

import com.google.gson.*;
import com.shirokai.core.styles.JsonValidation;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Layout only: actions are fixed server-side, never arbitrary configured commands. */
public record MenuConfig(int schemaVersion, boolean clanMenuEnabled, Profile profile) {
    public static final Set<String> ACTIONS=Set.of("header","previous","styles","clear","refresh","back","next");
    public record Button(int slot,String item,String name,List<String> lore){
        public Button{itemId(item);text(name);lore=lines(lore);}
    }
    public record Tags(String unlockedItem,String lockedItem,String name,List<String> lore){
        public Tags{itemId(unlockedItem);itemId(lockedItem);text(name);lore=lines(lore);}
    }
    public record Profile(int rows,String title,List<Integer> tagSlots,Map<String,Button> buttons,Tags tags){
        public Profile{
            if(rows<1||rows>6)throw new IllegalArgumentException("profile.rows deve estar entre1 e6");
            text(title);tagSlots=List.copyOf(tagSlots);buttons=Map.copyOf(buttons);Objects.requireNonNull(tags);
            if(!buttons.keySet().equals(ACTIONS))throw new IllegalArgumentException("Botões obrigatórios: "+ACTIONS);
            if(tagSlots.isEmpty())throw new IllegalArgumentException("Defina pelo menos um slot de tag");
            var occupied=new HashSet<Integer>();
            for(int slot:tagSlots)slot(slot,rows,occupied);
            for(var button:buttons.values())slot(button.slot(),rows,occupied);
        }
    }
    public MenuConfig{if(schemaVersion!=1||profile==null)throw new IllegalArgumentException("menus.schemaVersion deve ser1");}
    private static void slot(int slot,int rows,Set<Integer> occupied){if(slot<0||slot>=rows*9||!occupied.add(slot))throw new IllegalArgumentException("Slot fora do menu ou repetido: "+slot);}
    private static void itemId(String item){if(item==null||!item.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("ID de item inválido: "+item);}
    private static void text(String value){if(value==null||value.length()>512||value.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Texto do menu inválido/excede512 caracteres");}
    private static List<String> lines(List<String> value){if(value==null||value.size()>12)throw new IllegalArgumentException("Máximo12 linhas de descrição");value.forEach(MenuConfig::text);return List.copyOf(value);}
    private static Button button(int slot,String item,String name,String... lore){return new Button(slot,"minecraft:"+item,name,List.of(lore));}
    public static MenuConfig legacyDefaults(){
        var buttons=new LinkedHashMap<String,Button>();
        buttons.put("header",button(4,"player_head","<aqua>%menu:player%</aqua>","<gray>Cargo fixo: %menu:rank%</gray>","<gray>Clã: %menu:clan%</gray>","<gray>Tags opcionais: %menu:count%</gray>","<gray>Clique nas tags para equipar/remover no chat.</gray>"));
        buttons.put("previous",button(45,"arrow","<aqua>Página anterior</aqua>"));
        buttons.put("styles",button(47,"painting","<aqua>Estilo das tags</aqua>","<gray>Escolha as cores das tags opcionais.</gray>"));
        buttons.put("clear",button(48,"milk_bucket","<aqua>Remover tags opcionais</aqua>"));
        buttons.put("refresh",button(49,"clock","<aqua>Atualizar perfil</aqua>","<gray>Página %menu:page%</gray>"));
        buttons.put("back",button(50,"arrow","<yellow><bold>VOLTAR AO MENU</bold></yellow>","<gray>Retornar ao menu principal.</gray>","","<yellow>➜ Clique para voltar!</yellow>"));
        buttons.put("next",button(53,"arrow","<aqua>Próxima página</aqua>"));
        var slots=new ArrayList<Integer>();for(int slot=9;slot<45;slot++)slots.add(slot);
        return new MenuConfig(1,true,new Profile(6,"<gold><bold>✦ PERFIL SHIROKAI</bold></gold>",slots,buttons,
            new Tags("minecraft:name_tag","minecraft:barrier","%menu:tag%",List.of("<gray>%menu:description%</gray>","<gray>Estado: %menu:availability%</gray>","<gray>Categoria: %menu:category%</gray>","<gray>Exclusiva do chat</gray>","<gray>Chat: %menu:selection%</gray>"))));
    }
    public static MenuConfig defaults(){
        var old=legacyDefaults();var buttons=new LinkedHashMap<>(old.profile().buttons());
        var positions=Map.of("header",10,"styles",19,"back",28,"previous",36,"next",44,"clear",40,"refresh",38);
        buttons.replaceAll((key,b)->new Button(positions.get(key),b.item(),b.name(),b.lore()));
        var style=buttons.get("styles");buttons.put("styles",new Button(19,style.item(),"<aqua>Estilo de cores</aqua>",List.of("<gray>Esquerdo: escolher estilo.</gray>","<gray>Direito: remover tags equipadas.</gray>")));
        var head=buttons.get("header");var lore=new ArrayList<>(head.lore());lore.add("<gray>Clique para atualizar.</gray>");buttons.put("header",new Button(10,head.item(),head.name(),lore));
        return new MenuConfig(1,true,new Profile(5,old.profile().title(),PanelLayout.CONTENT,buttons,old.profile().tags()));
    }
    private static boolean legacyGeometry(Profile profile){
        var old=legacyDefaults().profile();
        return profile.rows()==old.rows()&&profile.tagSlots().equals(old.tagSlots())&&ACTIONS.stream().allMatch(key->profile.buttons().get(key).slot()==old.buttons().get(key).slot());
    }
    private static Profile migrateProfile(Profile profile){
        var old=legacyDefaults().profile();var target=defaults().profile();var buttons=new LinkedHashMap<String,Button>();
        profile.buttons().forEach((key,b)->{
            var next=target.buttons().get(key);
            buttons.put(key,b.equals(old.buttons().get(key))?next:new Button(next.slot(),b.item(),b.name(),b.lore()));
        });
        return new Profile(target.rows(),profile.title(),target.tagSlots(),buttons,profile.tags());
    }
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static String json(MenuConfig value){return GSON.toJson(value)+"\n";}
    public static MenuConfig load(Path file)throws IOException{
        Files.createDirectories(file.getParent());if(!Files.exists(file))try{Files.writeString(file,json(defaults()),StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored){}
        if(Files.size(file)>65536)throw new IOException("menus.json excede64KiB");
        try{
            String source=Files.readString(file);var parsed=parse(source);
            if(legacyGeometry(parsed.profile())){
                try{Files.writeString(file.resolveSibling(file.getFileName()+".pre-0.16.6.bak"),source,StandardOpenOption.CREATE_NEW);}catch(FileAlreadyExistsException ignored){}
                var migrated=new MenuConfig(parsed.schemaVersion(),parsed.clanMenuEnabled(),migrateProfile(parsed.profile()));
                Path temporary=Files.createTempFile(file.getParent(),"menus-",".tmp");
                try{Files.writeString(temporary,json(migrated));Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temporary);}
                return migrated;
            }
            return parsed;
        }catch(Exception error){Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();throw new IOException(file+": "+cause.getMessage(),error);}
    }
    public static MenuConfig parse(String source)throws IOException{
        var root=JsonValidation.object(source);JsonValidation.fields(root,Set.of("schemaVersion","clanMenuEnabled","profile"));JsonValidation.integer(root,"schemaVersion");JsonValidation.bool(root,"clanMenuEnabled");
        var p=object(root,"profile",Set.of("rows","title","tagSlots","buttons","tags"));JsonValidation.integer(p,"rows");JsonValidation.string(p,"title",false);
        if(!p.has("tagSlots")||!p.get("tagSlots").isJsonArray())throw new IllegalArgumentException("tagSlots deve ser lista");
        for(var slot:p.getAsJsonArray("tagSlots"))if(!slot.toString().matches("[0-9]{1,2}"))throw new IllegalArgumentException("Slot deve ser inteiro");
        var buttons=object(p,"buttons",ACTIONS);
        for(String key:ACTIONS){var b=object(buttons,key,Set.of("slot","item","name","lore"));JsonValidation.integer(b,"slot");JsonValidation.string(b,"item",false);JsonValidation.string(b,"name",false);JsonValidation.strings(b,"lore");}
        var tags=object(p,"tags",Set.of("unlockedItem","lockedItem","name","lore"));for(String key:List.of("unlockedItem","lockedItem","name"))JsonValidation.string(tags,key,false);JsonValidation.strings(tags,"lore");
        return GSON.fromJson(root,MenuConfig.class);
    }
    private static JsonObject object(JsonObject root,String key,Set<String> fields){if(!root.has(key)||!root.get(key).isJsonObject())throw new IllegalArgumentException("Objeto obrigatório: "+key);var result=root.getAsJsonObject(key);JsonValidation.fields(result,fields);return result;}
}
