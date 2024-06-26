package org.jqassistant.plugin.contextmapper.report;

import com.buschmais.jqassistant.core.report.api.model.Result;
import com.buschmais.jqassistant.core.rule.api.model.Concept;
import com.buschmais.jqassistant.core.rule.api.model.CypherExecutable;
import com.buschmais.jqassistant.core.rule.api.model.Report;
import com.buschmais.jqassistant.core.rule.api.model.RuleException;
import com.buschmais.jqassistant.core.rule.api.model.RuleSetBuilder;
import com.buschmais.jqassistant.core.rule.api.model.Severity;
import com.buschmais.jqassistant.core.scanner.api.DefaultScope;
import com.buschmais.jqassistant.core.test.plugin.AbstractPluginIT;
import org.jqassistant.plugin.contextmapper.scanner.InsuranceContextMapIT;
import org.junit.jupiter.api.Test;

import java.io.File;

public class ContextMapperReportPluginIT extends AbstractPluginIT {

    @Test
    public void scan() throws RuleException {
        File testFile = new File(getClassesDirectory(InsuranceContextMapIT.class), "Insurance-Example-Stage-1.cml");

        getScanner().scan(testFile, "Insurance-Example-Stage-1.cml", DefaultScope.NONE);

        Concept concept = Concept.builder()
                .id("context-mapper:BoundedContexts")
                .severity(Severity.MINOR)
                .report(Report.builder().selectedTypes(Report.selectTypes("context-mapper-diagram")).build())
                .executable(new CypherExecutable("MATCH (bC1:BoundedContext) OPTIONAL MATCH (bC1)-[d:DEFINES_DEPENDENCY]->(bC2:BoundedContext) RETURN bC1, d, bC2"))
                .build();

        ruleSet.getConceptBucket().add(RuleSetBuilder.newInstance().addConcept(concept).getRuleSet().getConceptBucket());
        Result<Concept> conceptResult = applyConcept("context-mapper:BoundedContexts");

    }

}
