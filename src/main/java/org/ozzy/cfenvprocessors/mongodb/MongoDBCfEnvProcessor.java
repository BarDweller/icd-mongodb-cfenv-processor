package org.ozzy.cfenvprocessors.mongodb;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfService;
import io.pivotal.cfenv.spring.boot.CfEnvProcessor;
import io.pivotal.cfenv.spring.boot.CfEnvProcessorProperties;

public class MongoDBCfEnvProcessor implements CfEnvProcessor {

    private static final Logger LOG = Logger.getLogger(MongoDBCfEnvProcessor.class.getName());

    public MongoDBCfEnvProcessor() {
    }

    @Override
    public boolean accept(CfService service) {
        boolean match = service.existsByLabelStartsWith("databases-for-mongodb");
        LOG.info("MongoDBCfEnvProcessor matched service entry : "+service.getName());
        return match;
    }

    @Override
    public CfEnvProcessorProperties getProperties() {
        return CfEnvProcessorProperties.builder()
        .propertyPrefixes("cfenv.processor.icdmongo,sslcontext,spring.data.mongodb")
        .serviceName("MongoDB")
        .build();
    }

    @Override
    public void process(CfCredentials cfCredentials, Map<String, Object> properties) {

        Map<String,Object> credentials = cfCredentials.getMap();

        String uri=null;
        String trustedcert = null;
        Map<String, Object> connection = (Map<String, Object>) credentials.get("connection");
        if (connection != null) {
            Map<String, Object> details = (Map<String, Object>) connection.get("mongodb");
            if (details != null) {
                List<String> uris = (List<String>) details.get("composed");
                if(uris.size()>0){
                    uri = uris.get(0);
                }
                Map<String, Object> certinfo = (Map<String, Object>) details.get("certificate");
                trustedcert = certinfo.get("certificate_base64").toString();
            }
        }

        if(uri !=null && trustedcert !=null){
            properties.put("spring.data.mongodb.uri", uri);

            properties.put("sslcontext.enabled", true);
            properties.put("sslcontext.contexts.mongodb.trustedcert", trustedcert);

            properties.put("cfenv.processor.icdmongo.enabled", true);
            properties.put("cfenv.processor.icdmongo.sslcontext", "mongodb");
        }else{
            LOG.warning("Unable to process vcap services entry for mongodb uri:"+(uri!=null)+" cert:"+(trustedcert!=null));
        }
    }
}