package com.omnicharge.user.common.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class FallbackLogReplayerTest {
    @Mock private LogEventPublisher logEventPublisher;
    @InjectMocks private FallbackLogReplayer replayer;

    @Test
    void testInit() {
        replayer.init();
    }
}
