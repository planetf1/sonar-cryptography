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
package com.ibm.plugin.rules.detection.bc.wrapper;

import com.ibm.engine.model.Size;
import com.ibm.engine.model.context.CipherContext;
import com.ibm.engine.model.factory.BlockSizeFactory;
import com.ibm.engine.model.factory.ValueActionFactory;
import com.ibm.engine.rule.IDetectionRule;
import com.ibm.engine.rule.builder.DetectionRuleBuilder;
import com.ibm.plugin.rules.detection.bc.BouncyCastleInfoMap;
import com.ibm.plugin.rules.detection.bc.blockcipher.BcBlockCipherEngine;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.sonar.plugins.java.api.tree.Tree;

public final class BcWrapperEngine {

    private BcWrapperEngine() {
        // nothing
    }

    private static BouncyCastleInfoMap infoMap = new BouncyCastleInfoMap();

    static {
        infoMap.putKey("AESWrapEngine");
        infoMap.putKey("AESWrapPadEngine").putName("AES");
        infoMap.putKey("ARIAWrapEngine");
        infoMap.putKey("ARIAWrapPadEngine").putName("ARIA");
        infoMap.putKey("CamelliaWrapEngine");
        infoMap.putKey("CryptoProWrapEngine");
        infoMap.putKey("DESedeWrapEngine");
        infoMap.putKey("GOST28147WrapEngine").putName("GOST 28147-89");
        infoMap.putKey("RC2WrapEngine");
        infoMap.putKey("SEEDWrapEngine");
    }

    private static @NotNull List<IDetectionRule<Tree>> simpleConstructors() {
        List<IDetectionRule<Tree>> constructorsList = new LinkedList<>();

        for (Map.Entry<String, BouncyCastleInfoMap.Info> entry : infoMap.entrySet()) {
            String engine = entry.getKey();
            String engineName = infoMap.getDisplayName(engine, "WrapEngine");

            constructorsList.add(
                    new DetectionRuleBuilder<Tree>()
                            .createDetectionRule()
                            .forObjectTypes("org.bouncycastle.crypto.engines." + engine)
                            .forConstructor()
                            .shouldBeDetectedAs(new ValueActionFactory<>(engineName))
                            // We want to capture all possible constructors (some have arguments)
                            .withAnyParameters()
                            .buildForContext(new CipherContext(CipherContext.Kind.WRAP_ENGINE))
                            .inBundle(() -> "bcWrapperEngine")
                            .withDependingDetectionRules(BcWrapperInit.rules()));
        }

        return constructorsList;
    }

    private static @NotNull List<IDetectionRule<Tree>> specialConstructors() {
        List<IDetectionRule<Tree>> constructorsList = new LinkedList<>();

        constructorsList.add(
                new DetectionRuleBuilder<Tree>()
                        .createDetectionRule()
                        .forObjectTypes("org.bouncycastle.crypto.engines.DSTU7624WrapEngine")
                        .forConstructor()
                        .shouldBeDetectedAs(new ValueActionFactory<>("DSTU 7624:2014"))
                        .withMethodParameter("int")
                        .shouldBeDetectedAs(new BlockSizeFactory<>(Size.UnitType.BIT))
                        .asChildOfParameterWithId(-1)
                        .buildForContext(new CipherContext(CipherContext.Kind.WRAP_ENGINE))
                        .inBundle(() -> "bcWrapperEngine")
                        .withDependingDetectionRules(BcWrapperInit.rules()));

        /*
         * The rules below bring specific information about the key wrap RFC specification.
         * The precise RFC does not need to be catpured, hence we use `JavaTranslator.UNKNOWN`,
         * that will then be replaced by the name of the actual child block cipher.
         */

        constructorsList.add(
                new DetectionRuleBuilder<Tree>()
                        .createDetectionRule()
                        .forObjectTypes("org.bouncycastle.crypto.engines.RFC5649WrapEngine")
                        .forConstructor()
                        .shouldBeDetectedAs(new ValueActionFactory<>("RFC 5649"))
                        .withMethodParameter("org.bouncycastle.crypto.BlockCipher")
                        .addDependingDetectionRules(BcBlockCipherEngine.rules())
                        .buildForContext(new CipherContext(CipherContext.Kind.WRAP_RFC))
                        .inBundle(() -> "bcWrapperEngine")
                        .withDependingDetectionRules(BcWrapperInit.rules()));

        constructorsList.add(
                new DetectionRuleBuilder<Tree>()
                        .createDetectionRule()
                        .forObjectTypes("org.bouncycastle.crypto.engines.RFC3394WrapEngine")
                        .forConstructor()
                        .shouldBeDetectedAs(new ValueActionFactory<>("RFC 3394"))
                        .withMethodParameter("org.bouncycastle.crypto.BlockCipher")
                        .addDependingDetectionRules(BcBlockCipherEngine.rules())
                        .buildForContext(new CipherContext(CipherContext.Kind.WRAP_RFC))
                        .inBundle(() -> "bcWrapperEngine")
                        .withDependingDetectionRules(BcWrapperInit.rules()));

        constructorsList.add(
                new DetectionRuleBuilder<Tree>()
                        .createDetectionRule()
                        .forObjectTypes("org.bouncycastle.crypto.engines.RFC3394WrapEngine")
                        .forConstructor()
                        .shouldBeDetectedAs(new ValueActionFactory<>("RFC 3394"))
                        .withMethodParameter("org.bouncycastle.crypto.BlockCipher")
                        .addDependingDetectionRules(BcBlockCipherEngine.rules())
                        .withMethodParameter("boolean")
                        .buildForContext(new CipherContext(CipherContext.Kind.WRAP_RFC))
                        .inBundle(() -> "bcWrapperEngine")
                        .withDependingDetectionRules(BcWrapperInit.rules()));

        return constructorsList;
    }

    @Unmodifiable
    @Nonnull
    public static List<IDetectionRule<Tree>> rules() {
        return Stream.of(simpleConstructors().stream(), specialConstructors().stream())
                .flatMap(i -> i)
                .toList();
    }
}
