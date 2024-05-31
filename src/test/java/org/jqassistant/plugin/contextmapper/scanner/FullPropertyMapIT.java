package org.jqassistant.plugin.contextmapper.scanner;

import com.buschmais.jqassistant.core.scanner.api.DefaultScope;
import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.jqassistant.core.test.plugin.AbstractPluginIT;
import lombok.Builder;
import lombok.Singular;
import org.jqassistant.plugin.contextmapper.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jqassistant.plugin.contextmapper.model.BoundedContextDependencyType.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FullPropertyMapIT extends AbstractPluginIT {

    @Test
    public void testFullPropertyExample() {
        store.beginTransaction();

        scanFileAndAssert("Full-Property-Example.cml");

        store.commitTransaction();
    }

    private ContextMapDescriptor scanFileAndAssert(String fileName) {
        File testFile = new File(getClassesDirectory(FullPropertyMapIT.class), fileName);
        Descriptor descriptor = getScanner().scan(testFile, fileName, DefaultScope.NONE);
        assertThat(descriptor).isInstanceOf(ContextMapperDescriptor.class);
        assertThat(descriptor).isInstanceOf(ContextMapperFileDescriptor.class);
        ContextMapperFileDescriptor contextMapper = (ContextMapperFileDescriptor) descriptor;
        assertThat(contextMapper.getContextMaps()).hasSize(1);
        assertContextMapNode(contextMapper.getContextMaps().get(0), "FullPropertyMap", "SYSTEM_LANDSCAPE", "TO_BE");
        assertThat(contextMapper.getContextMaps().get(0).getBoundedContexts()).hasSize(1);
        BoundedContextDescriptor boundedContextDescriptor = contextMapper.getContextMaps().get(0).getBoundedContexts().get(0);
        assertThat(boundedContextDescriptor.getDomainVisionStatement()).isEqualTo("Context 1 domainVisionStatement");
        assertThat(boundedContextDescriptor.getImplementationTechnology()).isEqualTo("Java, JEE Application");
        assertThat(boundedContextDescriptor.getResponsibilities()).containsExactlyInAnyOrderElementsOf(Arrays.asList("Responsibility 1", "Responsibility 2"));
        assertThat(boundedContextDescriptor.getKnowledgeLevel()).isEqualTo("CONCRETE");
        assertThat(contextMapper.getDomains()).hasSize(1);
        assertThat(contextMapper.getDomains().get(0).getName()).isEqualTo("Domain1");
        assertThat(contextMapper.getDomains().get(0).getDomainVisionStatement()).isEqualTo("Domain 1 domainVisionStatement");
        assertThat(contextMapper.getDomains().get(0).getSubdomains()).hasSize(1);
        assertThat(contextMapper.getDomains().get(0).getSubdomains().get(0).getName()).isEqualTo("Subdomain1");
        assertThat(contextMapper.getDomains().get(0).getSubdomains().get(0).getDomainVisionStatement()).isEqualTo("Subdomain 1 domainVisionStatement");
        return contextMapper.getContextMaps().get(0);
    }

    private void assertContextMapNode(ContextMapDescriptor contextMap, String name, String type, String state) {
        assertThat(contextMap.getName()).isEqualTo(name);
        assertThat(contextMap.getType()).isEqualTo(type);
        assertThat(contextMap.getState()).isEqualTo(state);
    }

}
