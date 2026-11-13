package com.shirokai.core.chat;

import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ChatSharesTest {
    @Test void emptySlotValidationRejectsBeforeDeliveryAndChecksOnlyAuthor(){
        var shares=new ChatShares();UUID author=UUID.randomUUID();
        shares.providers((id,slot)->{assertEquals(author,id);return slot==1?Optional.of(Text.literal("Entei")):Optional.empty();},id->Optional.empty());
        shares.validate(author,"@1 @carteira");
        var error=assertThrows(IllegalStateException.class,()->shares.validate(author,"Tenho @1 mas @2 vazio"));
        assertTrue(error.getMessage().contains("slot 2"));
        shares.validate(author,"@7 @12 email@1");
    }
    @Test void resolvesOnlyAuthorAndValidSlotsWithBoundedSnapshots(){
        var shares=new ChatShares();UUID author=UUID.randomUUID();var reads=new AtomicInteger();
        shares.providers((id,slot)->{assertEquals(author,id);assertEquals(1,slot);reads.incrementAndGet();return Optional.of(Text.literal("[Pikachu]"));},id->{assertEquals(author,id);return Optional.of(Text.literal("[Saldo 10]"));});
        assertEquals("Veja [Pikachu] [Pikachu] e [Saldo 10]",shares.render(author,"Veja @1 @1 e @carteira",Text::literal).getString());
        assertEquals(1,reads.get());
        assertEquals("@0 @7 @12 @carteiraBob mail@1 @@1",shares.render(author,"@0 @7 @12 @carteiraBob mail@1 @@1",Text::literal).getString());
        assertFalse(ChatShares.contains("@carteiraBob"));assertTrue(ChatShares.contains("(@6)!"));
    }
    @Test void colorsContinueAcrossTokenWithoutMutatingRenderedText(){
        var shares=new ChatShares();var rendered=LegacyChatColors.render("&aAntes @1 depois",net.minecraft.util.Formatting.WHITE);
        var result=shares.render(UUID.randomUUID(),"&aAntes @1 depois",ignored->rendered);
        boolean[] green={false};result.visit((style,part)->{if(part.contains("depois"))green[0]=style.getColor()!=null&&style.getColor().getRgb()==0x55ff55;return Optional.empty();},net.minecraft.text.Style.EMPTY);
        assertTrue(green[0]);assertEquals("Antes @1 depois",rendered.getString());
    }
    @Test void unavailableDataIsExplicitAndPlayerMarkupRemainsLiteral(){
        var shares=new ChatShares();
        assertEquals("<red>[Slot 6 vazio/indisponível] [Carteira indisponível]",shares.render(UUID.randomUUID(),"<red>@6 @CARTEIRA",Text::literal).getString());
        var count=new AtomicInteger();shares.providers((id,slot)->{count.incrementAndGet();return Optional.of(Text.literal("ok"));},id->Optional.empty());
        String result=shares.render(UUID.randomUUID(),"@1 @2 @3 @4 @5 @6 @1 @2 @3",Text::literal).getString();
        assertTrue(result.endsWith(" @3"));assertEquals(6,count.get());
    }
}
