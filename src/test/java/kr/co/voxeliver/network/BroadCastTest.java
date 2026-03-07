package kr.co.voxeliver.network;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.netty.channel.embedded.EmbeddedChannel;
import kr.co.voxeliver.network.protocol.impl.PingPacket;
import kr.co.voxeliver.network.session.PlayerSession;
import kr.co.voxeliver.network.session.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BroadCastTest {

    @AfterEach
    void tearDown() {
        SessionManager.clear();
    }

    @Test
    void sendWithExcludedSessionSkipsExcludedSession() {
        EmbeddedChannel firstChannel = new EmbeddedChannel();
        EmbeddedChannel secondChannel = new EmbeddedChannel();
        EmbeddedChannel thirdChannel = new EmbeddedChannel();

        try {
            PlayerSession first = new PlayerSession(firstChannel);
            PlayerSession second = new PlayerSession(secondChannel);
            PlayerSession third = new PlayerSession(thirdChannel);

            SessionManager.add(first);
            SessionManager.add(second);
            SessionManager.add(third);

            PingPacket packet = new PingPacket();
            BroadCast.send(packet, first);

            assertNull(firstChannel.readOutbound());
            assertSame(packet, secondChannel.readOutbound());
            assertSame(packet, thirdChannel.readOutbound());
        } finally {
            firstChannel.finishAndReleaseAll();
            secondChannel.finishAndReleaseAll();
            thirdChannel.finishAndReleaseAll();
        }
    }
}
