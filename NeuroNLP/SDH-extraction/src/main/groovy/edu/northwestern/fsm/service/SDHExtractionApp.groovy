package edu.northwestern.fsm.service

import com.opencsv.CSVReader
import com.opencsv.CSVWriter
import edu.northwestern.fsm.pipeline.SDHExtractionPipelineGenerator
import edu.northwestern.fsm.type.SDHSummary
import org.apache.uima.analysis_engine.AnalysisEngine
import org.apache.uima.fit.factory.AggregateBuilder
import org.apache.uima.jcas.JCas

class SDHExtractionApp {
    static class DocumentData {
        public String id
        public Boolean isolatedSDH
        public Integer shift
        public Integer thickness
        public Integer count
        public Integer side
        public Boolean convexity
        public String reportText
    }

    static Collection<DocumentData> readData(CSVReader reader) {
        Collection<DocumentData> documentData = []
        String [] nextLine
        def headers = reader.readNext()
        println(headers)
        while ((nextLine = reader.readNext()) != null) {
            DocumentData data = new DocumentData()
            data.id = nextLine[0]
            data.isolatedSDH = (nextLine[1] == 'true' ? true : false)
            data.shift = (nextLine[2] in ['-9', ''] ? null : nextLine[2].toInteger())
            data.thickness = (nextLine[3] in ['-9', ''] ? null : nextLine[3].toDouble())
            data.count = (nextLine[4] in ['-9', ''] ? null : nextLine[4].toInteger())
            data.side = (nextLine[5] in ['-9', ''] ? null : nextLine[5].toInteger())
            data.convexity = (nextLine[6] == 0 ? false : true)
            data.reportText = nextLine[7]
            documentData << data
        }
        return documentData
    }

    static void writeData(Collection<DocumentData> results, String outputFilename) {
        CSVWriter writer = new CSVWriter(new FileWriter(outputFilename))
        String [] headers = new String[5]
        headers[0] = "20092015ID"
        headers[1] = "side"
        headers[2] = "thickness"
        headers[3] = "count"
        headers[4] = "shift"
        writer.writeNext(headers)
        for (result in results) {
            String[] row = new String[5]
            row[0] = result.id
            row[1] = result.side
            row[2] = result.thickness
            row[3] = result.count
            row[4] = result.shift
            writer.writeNext(row)
        }
        writer.close()
    }

    AnalysisEngine engine
	JCas jcas

    /**
     * Constructor
     */
	SDHExtractionApp() {
        try {
            AggregateBuilder builder = SDHExtractionPipelineGenerator.createSDHPipeline()
            this.engine = builder.createAggregate()
            this.jcas = engine.newJCas()
        }
        catch (Exception e) { e.printStackTrace() }
    }

    void processDocuments(String inputFilename, String outputFilename) {
        Collection<DocumentData> documents = SDHExtractionApp.readData(new CSVReader(new FileReader(inputFilename)))
        Collection<DocumentData> results = []

        documents.each { DocumentData dd ->
            jcas.reset()
            jcas.setDocumentText(dd.reportText)
            SDHSummary summary = new SDHSummary(jcas)
            summary.documentTitle = dd.id
            summary.addToIndexes()
            engine.process(jcas)

            def result = new DocumentData()
            result.id = summary.documentTitle
            result.side = summary.side
            result.thickness = summary.thickness
            result.count = summary.count
            result.shift = summary.shift
            results << result
        }

        SDHExtractionApp.writeData(results, outputFilename)
    }

	/**
	 * Run the app
	 * @param args
	 */
	static void main(String[] args) {
        String inputFilename = '/Users/wthompson/Code/SDH NLP Development Project/SDH-coding.csv'
        String outputFilename = '/Users/wthompson/Code/SDH NLP Development Project/SDH-nlp-predictions.csv'
        SDHExtractionApp app = new SDHExtractionApp()
        app.processDocuments(inputFilename, outputFilename)
	}
}
