package com.bodeum.domain.ai.infrastructure.config;

import com.bodeum.domain.ai.infrastructure.indexing.AiSourceJpaEventListener;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class AiSourceJpaEventConfiguration implements HibernatePropertiesCustomizer {

    private final AiSourceJpaEventListener eventListener;

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put("hibernate.integrator_provider", (IntegratorProvider) () ->
                List.of(new Integrator() {
                    @Override
                    public void integrate(
                            Metadata metadata,
                            BootstrapContext bootstrapContext,
                            SessionFactoryImplementor sessionFactory
                    ) {
                        sessionFactory.getEventListenerRegistry()
                                .appendListeners(EventType.POST_INSERT, eventListener);
                        sessionFactory.getEventListenerRegistry()
                                .appendListeners(EventType.POST_UPDATE, eventListener);
                        sessionFactory.getEventListenerRegistry()
                                .appendListeners(EventType.POST_DELETE, eventListener);
                    }
                }));
    }
}
