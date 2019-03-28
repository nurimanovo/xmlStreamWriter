package com.msr.samples.xml;

import com.msr.samples.xml.beans.jaxb.FileInfo;
import com.msr.samples.xml.beans.jaxb.RecordType;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

class TestXmlWriter {
    private Path targetDir = Paths.get("./target/TestXmlWriter");

    @BeforeEach
    void setUp() throws IOException {
        if (Files.exists(targetDir)) {
            try {
                Files.walk(targetDir)
                        .filter(Files::isRegularFile)
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                throw new RuntimeWrapperException(e);
                            }
                        });
            } catch (RuntimeWrapperException e) {
                e.throwIfCauseIsAssignableFrom(IOException.class);
                throw e;
            }
        } else {
            Files.createDirectories(targetDir);
        }
    }

    /**
     * Тест записи 3 000 000 записей с сериализацией через JAXB без помещения всех записей в память
     */
    @Test
    void testWriteManyRecords() throws IOException, JAXBException, XMLStreamException {
        String fileName = UUID.randomUUID().toString() + ".xml";
        int recordCount = 3_000_000;
        Path targetFile = targetDir.resolve(fileName);
        // Создаем файл на запись
        try (OutputStream outputStream = Files.newOutputStream(targetFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            // Создаем Writer c rootElement <tns:file xmlns:tns="http://samples.msr.com/schemas/file">
            // Сериализуем через JAXB FileInfo.class, RecordType.class
            XMLWriter xmlWriter = XMLWriter.writer(outputStream, new QName("http://samples.msr.com/schemas/file", "file", "tns"), FileInfo.class, RecordType.class);
            // Заполняем FileInfo
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileSender("1");
            fileInfo.setRecordCount(recordCount);
            fileInfo.setVersion("1.1");
            // Сериализуем в unqualified тег file_info
            xmlWriter.element(fileInfo, QName.valueOf("file_info"));
            // Открываем unqualified тег records
            xmlWriter.beginXMLTag(QName.valueOf("records"));
            // Пишем 1 000 000 записей без размещения их в памяти
            // В целевом коде вместо цикла бежим по курсору в БД
            for (int i = 0; i < recordCount; i++) {
                // Создаем запись, в целевом коде получаем
                RecordType recordType = new RecordType();
                recordType.setRecordData(RandomStringUtils.randomAlphabetic(10, 20));
                recordType.setRecordPk(UUID.randomUUID().toString());
                // Пишем в xml по одной записи
                xmlWriter.element(recordType, QName.valueOf("record"));

            }
            // Закрываем все открытые теги и xml документ
            xmlWriter.close();
        }
    }


    /**
     * Обертка для {@linkplain Exception}, если из метода нельзя выбросовать {@linkplain Exception}
     */
    class RuntimeWrapperException extends RuntimeException {
        private static final long serialVersionUID = -4385289139236957217L;

        /**
         * @param cause ошибка
         */
        public RuntimeWrapperException(Exception cause) {
            super(cause);
        }

        /**
         * Проверка, что ошибка может быть назначена переменной типа {@code clazz}
         *
         * @param clazz класс ошибки
         * @return {@code true} - если ошибка может быть назначена переменной типа {@code clazz}, {@code false} - иначе
         */
        public boolean causeIsAssignableFrom(Class<? extends Exception> clazz) {
            return clazz.isAssignableFrom(getCause().getClass());
        }

        /**
         * Если изначальная ошибка является объектом типа {@code clazz}, то она выбрасывается
         *
         * @param clazz класс ошибки
         * @param <T>   наследник {@linkplain Exception}
         * @throws T ошибка
         */
        public <T extends Exception> void throwIfCauseIsAssignableFrom(Class<T> clazz) throws T {
            if (causeIsAssignableFrom(clazz)) {
                throw clazz.cast(getCause());
            }
        }
    }
}
