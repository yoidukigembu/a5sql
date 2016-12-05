package tech.hilo.a5sql.writer.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.CaseFormat;

import tech.hilo.a5sql.valueobject.Table;
import tech.hilo.a5sql.writer.TableWriter;

public class TableWriterImpl implements TableWriter {
	
	private static final String PACKAGE_FORMAT = "import %s;\n";
	
	private static final String TEMPLATE;
	
	private final Table table;
	
	public TableWriterImpl(Table table) {
		this.table = table;
	}

	@Override
	public void write(String packageName) throws IOException {
		String entityName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, table.getName());
		String packages = createPackages();
		String fields = createFields();
		
		String entity = TEMPLATE.replaceAll("\\{entityName\\}", entityName)
				.replaceAll("\\{imports\\}", packages)
				.replaceAll("\\{fields\\}", fields)
				.replaceAll("\\{tableName\\}", table.getName())
				.replaceAll("\\{package\\}", packageName);
		
		System.out.println(entity);
	}
	
	
	/**
	 * パッケージインポート作成
	 */
	private String createPackages() {
		List<String> packages = table.getColumns()
		.stream()
		.map(c -> c.getJavaType())
		.filter(type -> StringUtils.isNotBlank(type.getPackageName()))
		.distinct()
		.map(c -> String.format(PACKAGE_FORMAT, c.getPackageName()))
		.collect(Collectors.toList());
		return StringUtils.join(packages, "\n");
		
	}
	
	private String createFields() {
		StringBuilder sb = new StringBuilder();
		table.getColumns()
		.forEach(c -> {
			sb.append(String.format("/** %s */\n", c.getComment()));
			if (c.isId()) {
				sb.append("@Id\n");
			}
			if (c.isSerial()) {
				sb.append("@GeneratedValue(strategy = GenerationType.AUTO)\n");
			}
			sb.append(c.getAnnotation());
			sb.append("\n");
			sb.append(c.getField());
			sb.append("\n\n");
		});
		
		List<String> tabList = Arrays.asList(sb.toString().split("\\n"))
			.stream()
			.map(line -> StringUtils.isBlank(line) ? "" : "\t".concat(line))
			.collect(Collectors.toList());
			
		return StringUtils.join(tabList, "\n");
	}
	
	
	
	static {
		try (InputStream stream = TableWriterImpl.class.getClassLoader().getResourceAsStream("entityTemplate.txt")) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int val;
			while ((val = stream.read()) >= 0) {
				baos.write(val);
			}
			TEMPLATE = new String(baos.toByteArray());
			baos.close();
			stream.close();
		} catch (IOException e) {
			throw new RuntimeException("entityTemplate.txt が見つかりません。");
		}
	}

}
