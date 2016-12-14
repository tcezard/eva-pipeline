/*
 * Copyright 2015-2016 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.eva.pipeline.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.job.builder.FlowJobBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import uk.ac.ebi.eva.pipeline.jobs.flows.ParallelStatisticsAndAnnotationFlow;
import uk.ac.ebi.eva.pipeline.jobs.steps.VariantLoaderStep;
import uk.ac.ebi.eva.pipeline.listeners.VariantOptionsConfigurerListener;
import uk.ac.ebi.eva.pipeline.parameters.JobOptions;

/**
 * Complete pipeline workflow:
 * <p>
 * |--> (optionalStatisticsFlow: statsCreate --> statsLoad)
 * transform ---> load -+
 * |--> (optionalAnnotationFlow: variantsAnnotGenerateInput --> (annotationCreate --> annotationLoad))
 * <p>
 * Steps in () are optional
 */
@Configuration
@EnableBatchProcessing
@Import({VariantLoaderStep.class, ParallelStatisticsAndAnnotationFlow.class})
public class GenotypedVcfJob {
    private static final Logger logger = LoggerFactory.getLogger(GenotypedVcfJob.class);

    public static final String NAME_GENOTYPED_VCF_JOB = "genotyped-vcf-job";

    //job default settings
    private static final boolean INCLUDE_SAMPLES = true;

    private static final boolean COMPRESS_GENOTYPES = true;

    private static final boolean CALCULATE_STATS = false;

    private static final boolean INCLUDE_STATS = false;

    @Autowired
    @Qualifier(ParallelStatisticsAndAnnotationFlow.PARALLEL_STATISTICS_AND_ANNOTATION)
    private Flow parallelStatisticsAndAnnotation;

    @Autowired
    @Qualifier(VariantLoaderStep.NAME_LOAD_VARIANTS_STEP)
    private Step variantLoaderStep;

    @Autowired
    private JobOptions jobOptions;

    @Bean(NAME_GENOTYPED_VCF_JOB)
    @Scope("prototype")
    public Job genotypedVcfJob(JobBuilderFactory jobBuilderFactory) {
        logger.debug("Building '" + NAME_GENOTYPED_VCF_JOB + "'");

        JobBuilder jobBuilder = jobBuilderFactory
                .get(NAME_GENOTYPED_VCF_JOB)
                .incrementer(new RunIdIncrementer())
                .listener(genotypedJobListener());
        FlowJobBuilder builder = jobBuilder
                .flow(variantLoaderStep)
                .next(parallelStatisticsAndAnnotation)
                .end();

        return builder.build();
    }

    @Bean
    @Scope("prototype")
    public JobExecutionListener genotypedJobListener() {
        return new VariantOptionsConfigurerListener(INCLUDE_SAMPLES,
                COMPRESS_GENOTYPES,
                CALCULATE_STATS,
                INCLUDE_STATS);
    }
}
