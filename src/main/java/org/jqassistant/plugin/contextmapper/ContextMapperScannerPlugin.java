package org.jqassistant.plugin.contextmapper;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin.Requires;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import lombok.extern.slf4j.Slf4j;
import org.contextmapper.dsl.cml.CMLResource;
import org.contextmapper.dsl.contextMappingDSL.*;
import org.contextmapper.dsl.standalone.ContextMapperStandaloneSetup;
import org.contextmapper.dsl.standalone.StandaloneContextMapperAPI;
import org.eclipse.xtext.EcoreUtil2;
import org.jqassistant.plugin.contextmapper.model.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scanner plug-in to enrich the graph based on a .cml-file.
 *
 * @author Stephan Pirnbaum
 */
@Requires(FileDescriptor.class)
@Slf4j
public class ContextMapperScannerPlugin extends AbstractScannerPlugin<FileResource, ContextMapperDescriptor> {

    @Override
    public boolean accepts(FileResource fileResource, String path, Scope scope) {
        return path.toLowerCase().endsWith(".cml");
    }

    @Override
    public ContextMapperDescriptor scan(FileResource fileResource, String path, Scope scope, Scanner scanner) throws IOException {
        ScannerContext context = scanner.getContext();
        final Store store = context.getStore();

        StandaloneContextMapperAPI api = ContextMapperStandaloneSetup.getStandaloneAPI();
        CMLResource cml = api.loadCML(fileResource.getFile());

        ContextMapperFileDescriptor contextMapperDescriptor = store.addDescriptorType(context.getCurrentDescriptor(), ContextMapperFileDescriptor.class);

        List<DomainDescriptor> domains = EcoreUtil2.eAllOfType(cml.getContextMappingModel(), Domain.class).stream()
                .map(d -> processDomain(store, d, contextMapperDescriptor))
                .collect(Collectors.toList());

        List<BoundedContextDescriptor> boundedContexts = EcoreUtil2.eAllOfType(cml.getContextMappingModel(), BoundedContext.class).stream()
            .map(b -> processBoundedContexts(store, domains, b))
            .collect(Collectors.toList());

        EcoreUtil2.eAllOfType(cml.getContextMappingModel(), ContextMap.class).stream()
                .map(c -> processContextMap(store, c, boundedContexts))
                .forEach(c -> contextMapperDescriptor.getContextMaps().add(c));

        return contextMapperDescriptor;
    }

    private DomainDescriptor processDomain(Store store, Domain domain, ContextMapperFileDescriptor contextMapperFileDescriptor) {
        DomainDescriptor domainDescriptor = store.create(DomainDescriptor.class);
        contextMapperFileDescriptor.getDomains().add(domainDescriptor);
        domainDescriptor.setName(domain.getName());
        if (domain.getDomainVisionStatement() != null) {
            domainDescriptor.setDomainVisionStatement(domain.getDomainVisionStatement());
        }

        domain.getSubdomains().forEach(s -> {
            SubdomainDescriptor subdomainDescriptor = store.create(SubdomainDescriptor.class);
            subdomainDescriptor.setName(s.getName());
            if (s.getType() != null) {
                subdomainDescriptor.setType(s.getType().getName());
            }
            if (s.getDomainVisionStatement() != null) {
                subdomainDescriptor.setDomainVisionStatement(s.getDomainVisionStatement());
            }
            domainDescriptor.getSubdomains().add(subdomainDescriptor);
        });

        return domainDescriptor;
    }

    private BoundedContextDescriptor processBoundedContexts(Store store, List<DomainDescriptor> domains, BoundedContext boundedContext) {
        BoundedContextDescriptor boundedContextDescriptor = store.create(BoundedContextDescriptor.class);
        boundedContextDescriptor.setName(boundedContext.getName());
        if (boundedContext.getType() != null) {
            boundedContextDescriptor.setType(boundedContext.getType().getName());
        }
        if (boundedContext.getDomainVisionStatement() != null) {
            boundedContextDescriptor.setDomainVisionStatement(boundedContext.getDomainVisionStatement());
        }
        if (boundedContext.getImplementationTechnology() != null) {
            boundedContextDescriptor.setImplementationTechnology(boundedContext.getImplementationTechnology());
        }
        if (boundedContext.getResponsibilities() != null && !boundedContext.getResponsibilities().isEmpty()) {
            boundedContextDescriptor.setResponsibilities(boundedContext.getResponsibilities().toArray(String[]::new));
        }
        if (boundedContext.getKnowledgeLevel() != null) {
            boundedContextDescriptor.setKnowledgeLevel(boundedContext.getKnowledgeLevel().getLiteral());
        }
        boundedContext.getImplementedDomainParts().forEach(d -> {
            if (d instanceof Subdomain) {
                getSubdomainByName(domains, d.getName()).ifPresent(descriptor -> boundedContextDescriptor.getSubdomains().add(descriptor));
            } else if (d instanceof Domain) {
                getDomainByName(domains, d.getName()).ifPresent(descriptor -> boundedContextDescriptor.getDomains().add(descriptor));
            }
        });

        return boundedContextDescriptor;
    }

    private ContextMapDescriptor processContextMap(Store store, ContextMap contextMap, List<BoundedContextDescriptor> boundedContextDescriptors) {
        ContextMapDescriptor contextMapDescriptor = store.create(ContextMapDescriptor.class);
        contextMapDescriptor.setName(contextMap.getName());
        contextMapDescriptor.setType(contextMap.getType() != null ? contextMap.getType().getLiteral() : null);
        contextMapDescriptor.setState(contextMap.getState() != null ? contextMap.getState().getLiteral() : null);

        contextMap.getRelationships().forEach(r -> {
            if (r instanceof UpstreamDownstreamRelationship) {
                getBoundedContextByName(boundedContextDescriptors, contextMap.getName(), ((UpstreamDownstreamRelationship) r).getUpstream().getName()).ifPresent(uS -> {
                    getBoundedContextByName(boundedContextDescriptors, contextMap.getName(), ((UpstreamDownstreamRelationship) r).getDownstream().getName()).ifPresent(dS -> {
                        String[] sourceRoles = ((UpstreamDownstreamRelationship) r).getDownstreamRoles().stream().map(DownstreamRole::getLiteral).toArray(String[]::new);
                        String[] targetRoles = ((UpstreamDownstreamRelationship) r).getUpstreamRoles().stream().map(UpstreamRole::getLiteral).toArray(String[]::new);

                        createBoundedContextRelationship(store, dS, sourceRoles, uS, targetRoles, (r instanceof CustomerSupplierRelationship) ? BoundedContextDependencyType.CUSTOMER_SUPPLIER.getType() : BoundedContextDependencyType.UPSTREAM_DOWNSTREAM.getType());
                    });
                });
            } else if (r instanceof SharedKernel) {
                getBoundedContextByName(boundedContextDescriptors, contextMap.getName(), ((SharedKernel) r).getParticipant1().getName()).ifPresent(p1 -> {
                    getBoundedContextByName(boundedContextDescriptors, contextMap.getName(), ((SharedKernel) r).getParticipant2().getName()).ifPresent(p2 -> {
                        createBoundedContextRelationship(store, p1, p2, BoundedContextDependencyType.SHARED_KERNEL.getType());
                    });
                });
            } else if (r instanceof Partnership) {
                getBoundedContextByName(boundedContextDescriptors, contextMap.getName(), ((Partnership) r).getParticipant1().getName()).ifPresent(p1 -> {
                    getBoundedContextByName(boundedContextDescriptors, contextMap.getName(), ((Partnership) r).getParticipant2().getName()).ifPresent(p2 -> {
                        createBoundedContextRelationship(store, p1, p2, BoundedContextDependencyType.PARTNERSHIP.getType());
                    });
                });
            }
        });

        boundedContextDescriptors.forEach(b -> contextMapDescriptor.getBoundedContexts().add(b));

        return contextMapDescriptor;
    }

    private Optional<DomainDescriptor> getDomainByName(List<DomainDescriptor> domainDescriptors, String name) {
        return domainDescriptors.stream()
                .filter(d -> name.equals(d.getName()))
                .findFirst();
    }

    private Optional<SubdomainDescriptor> getSubdomainByName(List<DomainDescriptor> domainDescriptors, String name) {
        return domainDescriptors.stream()
                .flatMap(d -> d.getSubdomains().stream())
                .filter(s -> name.equals(s.getName()))
                .findFirst();
    }

    private Optional<BoundedContextDescriptor> getBoundedContextByName(List<BoundedContextDescriptor> boundedContextDescriptors, String contextMapName, String name) {
        if (name == null) {
            log.warn("Detected undefined Bounded Context in Relationship of Context Map {}, skipping Relationship.", contextMapName);
            return Optional.empty();
        }
        return boundedContextDescriptors.stream()
                .filter(bC -> name.equals(bC.getName()))
                .findFirst();
    }

    private void createBoundedContextRelationship(Store store, BoundedContextDescriptor source, BoundedContextDescriptor target, String type) {
        createBoundedContextRelationship(store, source, new String[0], target, new String[0], type);
    }

    private void createBoundedContextRelationship(Store store, BoundedContextDescriptor source, String[] sourceRules, BoundedContextDescriptor target, String[] targetRoles, String type) {
        BoundedContextDefinesDependency boundedContextDefinesDependency = store.create(source, BoundedContextDefinesDependency.class, target);
        boundedContextDefinesDependency.setType(type);
        boundedContextDefinesDependency.setSourceRoles(sourceRules);
        boundedContextDefinesDependency.setTargetRoles(targetRoles);
    }

}
