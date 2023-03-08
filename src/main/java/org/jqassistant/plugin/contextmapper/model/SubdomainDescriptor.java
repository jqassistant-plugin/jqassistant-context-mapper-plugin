package org.jqassistant.plugin.contextmapper.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import org.jqassistant.plugin.contextmapper.report.ContextMapperLanguage;

import static org.jqassistant.plugin.contextmapper.report.ContextMapperLanguage.ContextMapperLanguageElement.Subdomain;

/**
 * Descriptor for subdomains.
 *
 * @author Stephan Pirnbaum
 */
@ContextMapperLanguage(Subdomain)
@Label("Subdomain")
public interface SubdomainDescriptor extends ContextMapperDescriptor {

    String getName();

    void setName(String name);

    String getType();

    void setType(String type);
}
