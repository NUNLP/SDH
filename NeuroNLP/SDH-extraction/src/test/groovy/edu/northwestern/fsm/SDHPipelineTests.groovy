package edu.northwestern.fsm

import edu.northwestern.fsm.pipeline.SDHExtractionPipelineGenerator
import edu.northwestern.fsm.type.Section
import groovy.util.logging.Log4j
import org.apache.log4j.Level
import org.apache.log4j.PropertyConfigurator
import org.apache.uima.analysis_engine.AnalysisEngine
import org.apache.uima.analysis_engine.AnalysisEngineDescription
import org.apache.uima.cas.TypeSystem
import org.apache.uima.cas.impl.TypeSystemImpl
import org.apache.uima.fit.factory.AggregateBuilder
import org.apache.uima.jcas.JCas
import org.apache.uima.jcas.tcas.Annotation_Type
import org.apache.uima.json.JsonCasSerializer
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

@Log4j
class SDHPipelineTests {
    static AnalysisEngine engine

    @BeforeClass
    static void setupClass() {
        def config = new ConfigSlurper().parse(SDHPipelineTests.getResource('/log-config.groovy').text)
        PropertyConfigurator.configure(config.toProperties())
        AggregateBuilder builder = SDHExtractionPipelineGenerator.createSDHPipeline()
        AnalysisEngineDescription desc = builder.createAggregateDescription()
        engine = builder.createAggregate()
    }

    @Before
    void setup() throws Exception {
        log.setLevel(Level.INFO)
    }

    @Test void smokeTest() {
        File dir = new File('src/test/resources/data/input')
        OutputStream outputStream = new FileOutputStream(new File("src/test/resources/data/output/test.json"))
        dir.listFiles().each {
            JCas jcas = this.engine.newJCas()
            jcas.setDocumentText(it.text)
            jcas.setDocumentLanguage('en')
            engine.process(jcas)
            TypeSystem filterTypeSystem = new TypeSystemImpl()
            filterTypeSystem.addType(Section.canonicalName, Annotation_Type.typeIndexID)
            JsonCasSerializer.jsonSerialize(jcas.cas, filterTypeSystem, outputStream)
        }
    }
}
