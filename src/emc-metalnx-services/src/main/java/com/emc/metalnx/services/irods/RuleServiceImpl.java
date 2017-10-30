/*
 * Copyright (c) 2015-2017, Dell EMC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.emc.metalnx.services.irods;

import com.emc.metalnx.core.domain.entity.DataGridCollectionAndDataObject;
import com.emc.metalnx.core.domain.entity.DataGridResource;
import com.emc.metalnx.core.domain.entity.DataGridRule;
import com.emc.metalnx.core.domain.exceptions.DataGridConnectionRefusedException;
import com.emc.metalnx.core.domain.exceptions.DataGridRuleException;
import com.emc.metalnx.core.domain.utils.DataGridCoreUtils;
import com.emc.metalnx.services.interfaces.*;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.rule.IRODSRuleExecResult;
import org.irods.jargon.core.rule.IRODSRuleExecResultOutputParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.Map;

@Service("ruleService")
@Transactional
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.INTERFACES)
public class RuleServiceImpl implements RuleService {

    private static final Logger logger = LoggerFactory.getLogger(RuleServiceImpl.class);

    @Autowired
    CollectionService cs;

    @Autowired
    private IRODSServices is;

    @Autowired
    private ResourceService rs;

    @Autowired
    private ConfigService configService;

    public void execReplDataObjRule(String destResc, String path, boolean inAdminMode) throws DataGridRuleException, DataGridConnectionRefusedException {
        logger.info("Get Replication Rule called");

        String flags = String.format("destRescName=%s%s", destResc, inAdminMode ? "++++irodsAdmin=" : "");

        DataGridResource dgResc = rs.find(destResc);
        DataGridRule rule = new DataGridRule(DataGridRule.REPL_DATA_OBJ_RULE, dgResc.getHost());
        rule.setInputRuleParams(path, flags, "null");

        executeRule(rule.toString());
    }

    public void execPopulateMetadataRule(String host, String objPath) throws DataGridRuleException, DataGridConnectionRefusedException {
        if (!configService.isPopulateMsiEnabled()) return;

        logger.info("Get Populate Rule called");

        DataGridRule rule = new DataGridRule(DataGridRule.POPULATE_RULE, host);
        rule.setInputRuleParams(objPath);

        executeRule(rule.toString());
    }

    public void execImageRule(String host, String objPath, String filePath) throws DataGridRuleException, DataGridConnectionRefusedException {
        if (!DataGridCoreUtils.isImageFile(objPath)) return;

        logger.info("Get Image Rule called");

        DataGridRule rule = new DataGridRule(DataGridRule.JPG_RULE, host);
        rule.setInputRuleParams(objPath, filePath);

        executeRule(rule.toString());
    }

    public void execVCFMetadataRule(String host, String objPath, String filePath) throws DataGridRuleException, DataGridConnectionRefusedException {
        if (!DataGridCoreUtils.isVCFFile(objPath)) return;

        DataGridRule rule = new DataGridRule(DataGridRule.VCF_RULE, host);
        rule.setInputRuleParams(objPath, filePath);

        executeRule(rule.toString());
    }

    public void execBamCramMetadataRule(String host, String objPath, String filePath) throws DataGridRuleException, DataGridConnectionRefusedException {
        if (!DataGridCoreUtils.isBamOrCram(objPath)) return;

        logger.info("Get BAM/CRAM Rule called");

        DataGridRule rule = new DataGridRule(DataGridRule.BAM_CRAM_RULE, host);
        rule.setInputRuleParams(objPath, filePath);

        executeRule(rule.toString());
    }

    public void execManifestFileRule(String host, String targetPath, String objPath, String filePath) throws DataGridRuleException, DataGridConnectionRefusedException {
        if (!DataGridCoreUtils.isPrideXMLManifestFile(objPath)) return;

        logger.info("Get Manifest Rule called");

        DataGridRule rule = new DataGridRule(DataGridRule.XML_MANIFEST_RULE, host);

        List<DataGridCollectionAndDataObject> objs = cs.getSubCollectionsAndDataObjetsUnderPath(targetPath);

        for (DataGridCollectionAndDataObject obj : objs) {
            logger.info("Extracting metadata from [{}] and applying on [{}]", filePath, obj.getPath());
            rule.setInputRuleParams(obj.getPath(), filePath, filePath);
            executeRule(rule.toString());
        }
    }

    @Override
    public List<String> execGetMSIsRule(String host) throws DataGridConnectionRefusedException, DataGridRuleException {
        logger.info("Get Microservices Rule called");

        DataGridRule rule = new DataGridRule(DataGridRule.GET_MSIS_RULE, host);
        rule.setOutputRuleParams("msis");

        logger.debug(rule.toString());

        return DataGridCoreUtils.getMSIsAsList((String) executeRule(rule.toString()).get("*msis").getResultObject());
    }

    public String execGetVersionRule(String host) throws DataGridRuleException, DataGridConnectionRefusedException {
        logger.info("Get Version Rule called");

        DataGridRule rule = new DataGridRule(DataGridRule.GET_VERSION_RULE, host);
        rule.setOutputRuleParams("version");

        logger.debug(rule.toString());

        return (String) executeRule(rule.toString()).get("*version").getResultObject();
    }

    public void execIlluminaMetadataRule(DataGridResource dgResc, String targetPath, String objPath) throws DataGridRuleException, DataGridConnectionRefusedException {
        if (!DataGridCoreUtils.isIllumina(objPath)) return;

        logger.info("Illumina Rule called");

        String destResc = dgResc.getName();
        String host = dgResc.getHost();
        boolean declareOutputParams = false;

        DataGridRule tarRule = new DataGridRule(DataGridRule.TAR_RULE, host, declareOutputParams);
        tarRule.setInputRuleParams(objPath, targetPath, destResc);
        tarRule.setOutputRuleParams("Status");

        DataGridRule illuminaRule = new DataGridRule(DataGridRule.ILLUMINA_RULE, host, declareOutputParams);
        illuminaRule.setInputRuleParams(objPath, destResc);

        executeRule(tarRule.toString());
        executeRule(illuminaRule.toString());
    }

    @Override
    public void execEmptyTrashRule(String destResc, String objPath, boolean inAdminMode) throws DataGridConnectionRefusedException, DataGridRuleException {
        logger.info("Empty Trash Rule called");

        DataGridResource dgResc = rs.find(destResc);
        DataGridRule rule = new DataGridRule(DataGridRule.EMPTY_TRASH_RULE, dgResc.getHost(), false);

        String flag = inAdminMode ? "irodsAdminRmTrash=" : "irodsRmTrash=";

        rule.setInputRuleParams(objPath, flag);
        rule.setOutputRuleParams("out");

        executeRule(rule.toString());
    }

    @Override
    public void execDeploymentRule(String host, String ruleName, String ruleVaultPath)
            throws DataGridRuleException, DataGridConnectionRefusedException {
        logger.info("Deploy Rule called");

        if (ruleName == null || ruleName.isEmpty() || ruleVaultPath == null || ruleVaultPath.isEmpty() ||
                !is.isAtLeastIrods420()) return;

        DataGridRule rule = new DataGridRule(DataGridRule.DEPLOYMENT_RULE, host, false);

        rule.setInputRuleParams(ruleName, ruleVaultPath);
        executeRule(rule.toString());
    }

    @Override
    public Map<String, IRODSRuleExecResultOutputParameter> executeRule(String rule) throws DataGridRuleException, DataGridConnectionRefusedException {
        if (rule == null || rule.isEmpty()) return null;

        Map<String, IRODSRuleExecResultOutputParameter> ruleResultMap;

        try {
            IRODSRuleExecResult result = is.getRuleProcessingAO().executeRule(rule);
            ruleResultMap = result.getOutputParameterResults();
        } catch (JargonException e) {
            String error = String.format("Could not execute rule %s: %s", rule, e.getMessage());
            logger.error(error);
            throw new DataGridRuleException(error);
        }

        return ruleResultMap;
    }
}
