/*
 * SonarQube Cryptography Plugin
 * Copyright (C) 2024 IBM
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.plugin;

import com.ibm.output.cyclondx.CBOMOutputFileFactory;
import java.io.File;
import javax.annotation.Nonnull;
import org.sonar.api.batch.postjob.PostJob;
import org.sonar.api.batch.postjob.PostJobContext;
import org.sonar.api.batch.postjob.PostJobDescriptor;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class OutputFileJob implements PostJob {

    private static final Logger LOGGER = Loggers.get(OutputFileJob.class);

    @Override
    public void describe(PostJobDescriptor postJobDescriptor) {
        postJobDescriptor.name("Output generation");
    }

    @Override
    public void execute(@Nonnull PostJobContext postJobContext) {
        final String cbomFilename =
                postJobContext
                        .config()
                        .get(Constants.CBOM_OUTPUT_NAME)
                        .orElse(Constants.CBOM_OUTPUT_NAME_DEFAULT);
        ScannerManager scannerManager = new ScannerManager(new CBOMOutputFileFactory());
        scannerManager.getOutputFile().saveTo(new File(cbomFilename + ".json"));
        LOGGER.info("CBOM was successfully generated '" + cbomFilename + ".json'.");
    }
}
