package org.jqassistant.plugin.contextmapper.scanner;

import com.buschmais.jqassistant.core.scanner.api.DefaultScope;
import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.jqassistant.core.test.plugin.AbstractPluginIT;
import org.jqassistant.plugin.contextmapper.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UndefinedBoundedContextMapIT extends AbstractPluginIT {

    Map<String, BoundedContextDescriptor> bCDescriptors = new HashMap<>();

    @BeforeEach
    public void setup() {
        String[] bCnames = {"Mehl", "Sack"};

        for (String bCname : bCnames) {
            BoundedContextDescriptor bC = mock(BoundedContextDescriptor.class);
            when(bC.getName()).thenReturn(bCname);
            bCDescriptors.put(bCname, bC);
        }
    }

    @Test
    public void testUndefinedBoundedContext() {
        store.beginTransaction();
        ContextMapDescriptor contextMapDescriptor = scanFileAndAssert("Undefined-Bounded-Context.cml");

        Set<BoundedContextDefinesDependency> existingDependencies = new TreeSet<>(Comparator.comparing(o -> ((Long) o.getId())));
        for (BoundedContextDescriptor boundedContext : contextMapDescriptor.getBoundedContexts()) {
            existingDependencies.addAll(boundedContext.getSourceBoundedContextsDefines());
            existingDependencies.addAll(boundedContext.getTargetBoundedContextsDefines());
        }
        assertThat(existingDependencies).hasSize(0);

        store.commitTransaction();
    }

    private ContextMapDescriptor scanFileAndAssert(String fileName) {
        File testFile = new File(getClassesDirectory(UndefinedBoundedContextMapIT.class), fileName);
        Descriptor descriptor = getScanner().scan(testFile, fileName, DefaultScope.NONE);
        assertThat(descriptor).isInstanceOf(ContextMapperDescriptor.class);
        assertThat(descriptor).isInstanceOf(ContextMapperFileDescriptor.class);
        ContextMapperFileDescriptor contextMapper = (ContextMapperFileDescriptor) descriptor;
        assertThat(contextMapper.getContextMaps()).hasSize(1);
        assertContextMapNode(contextMapper.getContextMaps().get(0), "Muehle", "SYSTEM_LANDSCAPE", "TO_BE");
        assertThat(contextMapper.getContextMaps().get(0).getBoundedContexts()).usingElementComparator(Comparator.comparing(BoundedContextDescriptor::getName)).containsExactlyInAnyOrderElementsOf(bCDescriptors.values());
        return contextMapper.getContextMaps().get(0);
    }

    private void assertContextMapNode(ContextMapDescriptor contextMap, String name, String type, String state) {
        assertThat(contextMap.getName()).isEqualTo(name);
        assertThat(contextMap.getType()).isEqualTo(type);
        assertThat(contextMap.getState()).isEqualTo(state);
    }

}
