package org.adnan.travner.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    private static final String STRONG_PASSWORD = "Str0ng!Bootstrap#Pass";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer initializer;

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(initializer, "bootstrapUsername", "");
        ReflectionTestUtils.setField(initializer, "bootstrapPassword", "");
        ReflectionTestUtils.setField(initializer, "bootstrapEmail", "admin@travner.local");

        logger = (Logger) LoggerFactory.getLogger(DataInitializer.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void doesNothingWhenNotConfigured() {
        initializer.run();

        verifyNoInteractions(userRepository);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void doesNothingWhenOnlyUsernameIsConfigured() {
        ReflectionTestUtils.setField(initializer, "bootstrapUsername", "bootstrapadmin");

        initializer.run();

        verifyNoInteractions(userRepository);
    }

    @Test
    void doesNotCreateAdminWithWeakPassword() {
        configure("bootstrapadmin", "admin123");

        initializer.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void doesNotCreateAdminWithInvalidUsername() {
        configure("not a valid username!", STRONG_PASSWORD);

        initializer.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void createsAdminWhenConfiguredAndNoAdminExists() {
        configure("bootstrapadmin", STRONG_PASSWORD);
        when(userRepository.countAdmins()).thenReturn(0L);
        when(passwordEncoder.encode(STRONG_PASSWORD)).thenReturn("encoded-hash");

        initializer.run();

        ArgumentCaptor<UserEntry> captor = ArgumentCaptor.forClass(UserEntry.class);
        verify(userRepository).save(captor.capture());
        UserEntry created = captor.getValue();
        assertThat(created.getUserName()).isEqualTo("bootstrapadmin");
        assertThat(created.getPassword()).isEqualTo("encoded-hash");
        assertThat(created.getRoles()).containsExactly("USER", "ADMIN");
        assertThat(created.isActive()).isTrue();
        assertThat(created.getCreatedAt()).isNotNull();
    }

    @Test
    void neverLogsTheBootstrapPassword() {
        configure("bootstrapadmin", STRONG_PASSWORD);
        when(userRepository.countAdmins()).thenReturn(0L);
        when(passwordEncoder.encode(STRONG_PASSWORD)).thenReturn("encoded-hash");

        initializer.run();

        String allLogMessages = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", String::concat);
        assertThat(allLogMessages).doesNotContain(STRONG_PASSWORD).doesNotContain("encoded-hash");
    }

    @Test
    void doesNotCreateAdminWhenOneAlreadyExists() {
        configure("bootstrapadmin", STRONG_PASSWORD);
        when(userRepository.countAdmins()).thenReturn(1L);

        initializer.run();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void doesNotCreateAdminWhenUsernameIsTaken() {
        configure("bootstrapadmin", STRONG_PASSWORD);
        when(userRepository.countAdmins()).thenReturn(0L);
        when(userRepository.findByuserName("bootstrapadmin")).thenReturn(new UserEntry());

        initializer.run();

        verify(userRepository, never()).save(any());
    }

    private void configure(String username, String password) {
        ReflectionTestUtils.setField(initializer, "bootstrapUsername", username);
        ReflectionTestUtils.setField(initializer, "bootstrapPassword", password);
    }
}
