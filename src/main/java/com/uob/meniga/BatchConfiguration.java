package com.uob.meniga;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.RowMapper;

import com.uob.meniga.model.BatchJobConfig;
import com.uob.meniga.model.Data;
import com.uob.meniga.util.CommonUtil;
import com.uob.meniga.util.HeaderFooterWriter;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {
	
	@Autowired
	public JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	public StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	public DataSource dataSource;
	
    @Autowired
    BatchJobConfig configuration;
	
	
	public class DataRowMapper implements RowMapper<Data>{
		@Override
		public Data mapRow(ResultSet rs, int rowNum) throws SQLException{
			Data data = new Data();
			String dataOutput = CommonUtil.removeNull(rs,configuration.getDelimiter());
			data.setOutputValue(dataOutput);
			return data;
		}
	}
	
	public JdbcCursorItemReader<Data> dataReader(){
		JdbcCursorItemReader<Data> reader = new JdbcCursorItemReader<Data>();
		reader.setDataSource(dataSource);
		reader.setSql(configuration.getQuery());
		reader.setRowMapper(new DataRowMapper());
		return reader;
	}
	
    @Bean
    public ItemProcessor<Data, String> dataProcessor() {
        return new DataItemProcessor();
    }
    
    @Autowired
    HeaderFooterWriter hfwriter = new HeaderFooterWriter();
    

	public FlatFileItemWriter<String> dataWriter(HeaderFooterWriter hfwriter){
		FlatFileItemWriter<String> writer = new FlatFileItemWriter<String>();
		
		writer.setHeaderCallback(hfwriter);
		writer.setFooterCallback(hfwriter);
		writer.setResource(new FileSystemResource(new File(configuration.getOutputFolder()+configuration.getOutputFileName())));
		writer.setLineAggregator(new PassThroughLineAggregator<String>());
		
		return writer;
	}
	
	
	@Bean
	public Step step1() {
		return stepBuilderFactory.get("step1").<Data, String> chunk(10000)
				.reader(dataReader())
				.processor(dataProcessor())
				.writer(dataWriter(hfwriter))
				.listener(hfwriter)
				.build()
				;
	}
	
	
	@Bean
	public Job exportUserJob() {
		return jobBuilderFactory.get("JDBCextractorJob")
				.incrementer(new RunIdIncrementer())
				.flow(step1())
				.end()
				.build();
	}
}
