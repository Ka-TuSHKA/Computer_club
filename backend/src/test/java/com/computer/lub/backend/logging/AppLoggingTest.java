package com.computer.lub.backend.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.computer.lub.backend.aspect.LoggingAspect;
import com.computer.lub.backend.event.AuditEvent;
import com.computer.lub.backend.event.AuditEventListener;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppLoggingTest {

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        listAppender = new ListAppender<>();
        listAppender.start();
    }

    // Очистка после каждого теста
    @AfterEach
    void tearDown() {
        listAppender.stop();
        listAppender.list.clear();
    }

    @Nested
    @DisplayName("Тесты LoggingAspect")
    class AspectTests {

        @Mock private ProceedingJoinPoint joinPoint;
        @Mock private Signature signature;

        private LoggingAspect aspect;
        private Logger aspectLogger;

        @BeforeEach
        void setUpAspect() {
            aspect = new LoggingAspect();
            aspectLogger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
            aspectLogger.addAppender(listAppender); // Прикрепляем перехватчик
        }

        @AfterEach
        void tearDownAspect() {
            aspectLogger.detachAppender(listAppender); // Открепляем
        }

        @Test
        @DisplayName("Должен логировать успешный вход и выход из метода")
        void shouldLogMethodEntryAndExit() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.toShortString()).thenReturn("TestService.doWork(..)");
            when(joinPoint.proceed()).thenReturn("OK");

            Object result = aspect.log(joinPoint);

            assertEquals("OK", result);
            assertEquals(2, listAppender.list.size(), "Должно быть 2 записи: вход и выход");

            ILoggingEvent enterLog = listAppender.list.get(0);
            assertEquals(Level.INFO, enterLog.getLevel());
            assertEquals("-> TestService.doWork(..)", enterLog.getFormattedMessage());

            ILoggingEvent exitLog = listAppender.list.get(1);
            assertEquals(Level.INFO, exitLog.getLevel());
            assertTrue(exitLog.getFormattedMessage().startsWith("<- TestService.doWork(..)"));
        }

        @Test
        @DisplayName("Должен логировать ошибку уровня WARN при исключении")
        void shouldLogWarningOnException() throws Throwable {
            when(joinPoint.getSignature()).thenReturn(signature);
            when(signature.toShortString()).thenReturn("TestService.fail(..)");
            when(joinPoint.proceed()).thenThrow(new RuntimeException("Boom"));

            assertThrows(RuntimeException.class, () -> aspect.log(joinPoint));

            assertEquals(2, listAppender.list.size());
            ILoggingEvent errorLog = listAppender.list.get(1);
            assertEquals(Level.WARN, errorLog.getLevel());
            assertEquals("X TestService.fail(..) — Boom", errorLog.getFormattedMessage());
        }
    }

    @Nested
    @DisplayName("Тесты AuditEventListener")
    class AuditTests {

        private AuditEventListener listener;
        private Logger auditLogger;

        @BeforeEach
        void setUpAudit() {
            listener = new AuditEventListener();
            auditLogger = (Logger) LoggerFactory.getLogger("AUDIT");
            auditLogger.addAppender(listAppender);
        }

        @AfterEach
        void tearDownAudit() {
            auditLogger.detachAppender(listAppender);
        }

        @Test
        @DisplayName("Должен корректно записывать событие аудита")
        void shouldLogAuditEvent() {
            AuditEvent event = new AuditEvent(this, "LOGIN", "admin", "role=ADMIN");

            listener.onAudit(event);

            assertEquals(1, listAppender.list.size());
            ILoggingEvent logEvent = listAppender.list.get(0);

            assertEquals(Level.INFO, logEvent.getLevel());
            assertEquals("LOGIN | user=admin | role=ADMIN", logEvent.getFormattedMessage());
            // Проверяем, что используется именно логгер AUDIT
            assertEquals("AUDIT", logEvent.getLoggerName());
        }

        @Test
        @DisplayName("Должен корректно обрабатывать пустые детали")
        void shouldHandleEmptyDetails() {
            AuditEvent event = new AuditEvent(this, "LOGOUT", "client", "");

            listener.onAudit(event);

            ILoggingEvent logEvent = listAppender.list.get(0);
            assertEquals("LOGOUT | user=client | ", logEvent.getFormattedMessage());
        }
    }
}
