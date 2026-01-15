package io.github.jsbxyyx.jcurl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 使用 httpbin.org 进行文件上传测试
 */
public class FileUploadTest {

    private static final String HTTPBIN_POST = "https://httpbin.org/post";
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    @Test
    void test() {
        try {
            System.out.println("==================== 准备测试环境 ====================\n");
            setupTestFiles();

            System.out.println("\n==================== 开始文件上传测试 ====================\n");

            // 测试 1: 单个文本文件上传
            test1_SingleTextFileUpload();

            // 测试 2: 单个二进制文件上传
            test2_SingleBinaryFileUpload();

            // 测试 3: 多个文件上传
            test3_MultipleFilesUpload();

            // 测试 4: 文件 + 文本���段混合
            test4_MixedFormData();

            // 测试 5: 自定义文件名和 Content-Type
            test5_CustomFileMetadata();

            // 测试 6: 使用 curl 命令风格
            test6_CurlStyleUpload();

            // 测试 7: 大文件上传
            test7_LargeFileUpload();

            // 测试 8: 空文件上传
            test8_EmptyFileUpload();

            // 测试 9: 特殊字符文件名
            test9_SpecialCharacterFilename();

            // 测试 10: 多个同名字段
            test10_MultipleFieldsWithSameName();

            System.out.println("\n==================== 测试总结 ====================");
            System.out.println("✓ 通过:  " + testsPassed);
            System.out.println("✗ 失败: " + testsFailed);
            System.out.println("总计: " + (testsPassed + testsFailed));
            System.out.println("================================================\n");

            cleanupTestFiles();

        } catch (Exception e) {
            System.err.println("测试执行出错:  " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 准备测试文件
     */
    private static void setupTestFiles() throws IOException {
        System.out.println("创建测试文件.. .");

        // 1. 文本文件
        String textContent = "Hello, World!\nThis is a test file for JCurl.\n文件上传测试。";
        Files.write(Paths.get("test.txt"), textContent.getBytes(StandardCharsets.UTF_8));
        System.out.println("  ✓ test.txt (文本文件)");

        // 2. JSON 文件
        String jsonContent = "{\n  \"name\": \"JCurl\",\n  \"version\": \"1.0\",\n  \"author\": \"测试\"\n}";
        Files.write(Paths.get("data.json"), jsonContent.getBytes(StandardCharsets.UTF_8));
        System.out.println("  ✓ data.json (JSON 文件)");

        // 3. 模拟图片文件 (PNG header + random data)
        byte[] pngData = new byte[2048];
        // PNG 文件头
        pngData[0] = (byte) 0x89;
        pngData[1] = 0x50;
        pngData[2] = 0x4E;
        pngData[3] = 0x47;
        pngData[4] = 0x0D;
        pngData[5] = 0x0A;
        pngData[6] = 0x1A;
        pngData[7] = 0x0A;
        // 填充随机数据
        for (int i = 8; i < pngData.length; i++) {
            pngData[i] = (byte) (Math.random() * 256);
        }
        Files.write(Paths.get("image.png"), pngData);
        System.out.println("  ✓ image.png (模拟图片, 2KB)");

        // 4. 二进制文件
        byte[] binaryData = new byte[1024];
        for (int i = 0; i < binaryData.length; i++) {
            binaryData[i] = (byte) (i % 256);
        }
        Files.write(Paths.get("binary.dat"), binaryData);
        System.out.println("  ✓ binary.dat (二进制文件, 1KB)");

        // 5. 大文件
        byte[] largeData = new byte[1024 * 50]; // 50KB
        Files.write(Paths.get("large.bin"), largeData);
        System.out.println("  ✓ large.bin (大文件, 50KB)");

        // 6. 空文件
        Files.write(Paths.get("empty.txt"), new byte[0]);
        System.out.println("  ✓ empty.txt (空文件)");

        // 7. 特殊字符文件名
        String specialContent = "文件名包含特殊字符";
        Files.write(Paths.get("特殊文件名-测试.txt"), specialContent.getBytes(StandardCharsets.UTF_8));
        System.out.println("  ✓ 特殊文件名-测试.txt (特殊字符文件名)");

        System.out.println("\n所有测试文件创建完成！");
    }

    /**
     * 测试 1: 单个文本文件上传
     */
    private static void test1_SingleTextFileUpload() {
        System.out.println("【测试 1】单个文本文件上传");
        System.out.println("----------------------------------------");

        try {
            JCurl.HttpResponseModel response = JCurl.create()
                    .url(HTTPBIN_POST)
                    .post()
                    .formFile("file", "test.txt")
                    .exec();

            boolean success = validateResponse(response, "Hello, World!");

            printResult("单个文本文件上传", success, response);

        } catch (IOException e) {
            printError("单个文本文件上传", e);
        }
    }

    /**
     * 测试 2: 单个二进制文件上传
     */
    private static void test2_SingleBinaryFileUpload() {
        System.out.println("\n【测试 2】单个二进制文件上传");
        System.out.println("----------------------------------------");

        try {
            JCurl.HttpResponseModel response = JCurl.create()
                    .url(HTTPBIN_POST)
                    .post()
                    .formFile("binaryFile", "binary.dat")
                    .exec();

            boolean success = validateResponse(response, "binaryFile");

            printResult("单个二进制文件上传", success, response);

        } catch (IOException e) {
            printError("单个二进制文件上传", e);
        }
    }

    /**
     * 测试 3: 多个文件上传
     */
    private static void test3_MultipleFilesUpload() {
        System.out.println("\n【测试 3】多个文件上传");
        System.out.println("----------------------------------------");

        try {
            JCurl.HttpResponseModel response = JCurl.create()
                    .url(HTTPBIN_POST)
                    .post()
                    .formFile("textFile", "test.txt")
                    .formFile("jsonFile", "data.json")
                    .formFile("imageFile", "image.png")
                    .exec();

            boolean success = validateResponse(response,
                    "imageFile", "jsonFile", "imageFile");

            printResult("多个文件上传", success, response);

        } catch (IOException e) {
            printError("多个文件上传", e);
        }
    }

    /**
     * 测试 4: 文件 + 文本字段混合
     */
    private static void test4_MixedFormData() {
        System.out.println("\n【测试 4】文件 + 文本字段混合");
        System.out.println("----------------------------------------");

        try {
            JCurl.HttpResponseModel response = JCurl.create()
                    .url(HTTPBIN_POST)
                    .post()
                    .formField("username", "张三")
                    .formField("email", "zhangsan@example.com")
                    .formField("age", "25")
                    .formField("description", "这是一个测试用户")
                    .formFile("avatar", "image.png")
                    .formFile("document", "data.json")
                    .exec();

            boolean success = validateResponse(response,
                    "username", "email", "age",
                    "avatar", "document");

            printResult("文件 + 文本字段混合", success, response);

        } catch (IOException e) {
            printError("文件 + 文本字段混合", e);
        }
    }

    /**
     * 测试 5: 自定义文件名和 Content-Type
     */
    private static void test5_CustomFileMetadata() {
        System.out.println("\n【测试 5】自定义文件名和 Content-Type");
        System.out.println("----------------------------------------");

        try {
            JCurl.HttpResponseModel response = JCurl.create()
                    .url(HTTPBIN_POST)
                    .post()
                    .formFile("upload1", "image.png", "custom-avatar.png", "image/png")
                    .formFile("upload2", "data.json", "config.json", "application/json")
                    .formFile("upload3", "test.txt", "readme.txt", "text/plain")
                    .exec();

            boolean success = validateResponse(response,
                    "upload1", "upload2", "upload3");

            printResult("自定义文件名和 Content-Type", success, response);

        } catch (IOException e) {
            printError("自定义文件名和 Content-Type", e);
        }
    }

    /**
     * 测试 6: 使用 curl 命令风格
     */
    private static void test6_CurlStyleUpload() {
        System.out.println("\n【测试 6】使用 curl 命令风格");
        System.out.println("----------------------------------------");

        try {
            // 方式 1: 使用 opt() 方法
            JCurl.HttpResponseModel response1 = JCurl.create()
                    .url(HTTPBIN_POST)
                    .opt("-X", "POST")
                    .opt("-F", "name=测试用户")
                    .opt("-F", "file=@test.txt")
                    .opt("-F", "image=@image.png")
                    .exec();

            boolean success1 = validateResponse(response1, "name", "file", "image");
            System.out.println("  方式1 (opt方法): " + (success1 ? "✓" : "✗"));

            // 方式 2: 从 curl 命令字符串解析
            String curlCommand = "curl -X POST " + HTTPBIN_POST + " " +
                    "-F 'username=John' " +
                    "-F 'email=john@example.com' " +
                    "-F 'avatar=@image.png'";

            JCurl.HttpResponseModel response2 = JCurl.fromCurl(curlCommand).exec();

            boolean success2 = validateResponse(response2, "username", "email", "avatar");
            System.out.println("  方式2 (fromCurl): " + (success2 ? "✓" : "✗"));

            printResult("curl 命令风格", success1 && success2, response2);

        } catch (IOException e) {
            printError("curl 命令风格", e);
        }
    }

    /**
     * 测试 7: 大文件上传
     */
    private static void test7_LargeFileUpload() {
        System.out.println("\n【测试 7】大文件上传");
        System.out.println("----------------------------------------");

        try {
            long startTime = System.currentTimeMillis();

            JCurl.HttpResponseModel response = JCurl.create()
                    .url(HTTPBIN_POST)
                    .post()
                    .formFile("largefile", "large.bin")
                    .formField("description", "这是一个大文件测试")
                    .connectTimeout(30000)
                    .readTimeout(30000)
                    .exec();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            boolean success = validateResponse(response, "largefile", "description");
            System.out.println("  上传耗时: " + duration + "ms");
            System.out.println("  文件大小: 50KB");

            printResult("大文件上传", success, response);

        } catch (IOException e) {
            printError("大文件上传", e);
        }
    }

    /**
     * 测试 8: 空文件上传
     */
    private static void test8_EmptyFileUpload() {
        System.out.println("\n【测试 8】空文件上传");
        System.out.println("----------------------------------------");

        try {
            JCurl.HttpResponseModel response = JCurl.create()
                    .url(HTTPBIN_POST)
                    .post()
                    .formFile("emptyfile", "empty.txt")
                    .formField("note", "这是一个空文件")
                    .exec();

            boolean success = validateResponse(response, "emptyfile", "note");

            printResult("空文件上传", success, response);

        } catch (IOException e) {
            printError("空文件上传", e);
        }
    }

    /**
     * 测试 9: 特殊字符文件名
     */
    private static void test9_SpecialCharacterFilename() {
        System.out.println("\n【测试 9】特殊字符文件名");
        System.out.println("----------------------------------------");

        try {
            JCurl.HttpResponseModel response = JCurl.create()
                    .url(HTTPBIN_POST)
                    .post()
                    .formFile("file", "特殊文件名-测试.txt")
                    .exec();

            boolean success = response.getStatusCode() == 200;

            printResult("特殊字符文件名", success, response);

        } catch (IOException e) {
            printError("特殊字符文件名", e);
        }
    }

    /**
     * 测试 10: 多个同名字段
     */
    private static void test10_MultipleFieldsWithSameName() {
        System.out.println("\n【测试 10】多个同名字段");
        System.out.println("----------------------------------------");

        try {
            JCurl.HttpResponseModel response = JCurl.create()
                    .url(HTTPBIN_POST)
                    .post()
                    .formField("tags", "java")
                    .formField("tags", "http")
                    .formField("tags", "upload")
                    .formFile("files", "test.txt")
                    .formFile("files", "data.json")
                    .exec();

            boolean success = validateResponse(response, "java", "http", "upload", "files");

            printResult("多个同名字段", success, response);

        } catch (IOException e) {
            printError("多个同名字段", e);
        }
    }

    /**
     * 验证响应
     */
    private static boolean validateResponse(JCurl.HttpResponseModel response, String... expectedContents) {
        if (response.getStatusCode() != 200) {
            return false;
        }

        String body = response.getBody();
        if (body == null || body.isEmpty()) {
            return false;
        }

        // 检查所有期望的内容是否都存在
        for (String expected : expectedContents) {
            if (!body.contains(expected)) {
                System.out.println("  ✗ 响应中未找到:  " + expected);
                return false;
            }
        }

        return true;
    }

    /**
     * 打印测试结果
     */
    private static void printResult(String testName, boolean success, JCurl.HttpResponseModel response) {
        if (success) {
            testsPassed++;
            System.out.println("\n✓ " + testName + " - 通过");
            System.out.println("  状态码: " + response.getStatusCode());
            System.out.println("  响应大小: " + response.getBody().length() + " 字节");
        } else {
            testsFailed++;
            System.out.println("\n✗ " + testName + " - 失败");
            System.out.println("  状态码: " + response.getStatusCode());
            System.out.println("  状态消息: " + response.getStatusMessage());
            if (response.getBody() != null && response.getBody().length() < 500) {
                System.out.println("  响应体: " + response.getBody());
            }
        }
    }

    /**
     * 打印错误信息
     */
    private static void printError(String testName, Exception e) {
        testsFailed++;
        System.out.println("\n✗ " + testName + " - 异常");
        System.out.println("  错误: " + e.getMessage());
    }

    /**
     * 清理测试文件
     */
    private static void cleanupTestFiles() {
        System.out.println("清理测试文件...");

        String[] files = {
                "test.txt",
                "data.json",
                "image.png",
                "binary.dat",
                "large.bin",
                "empty.txt",
                "特殊文件名-测试.txt"
        };

        int deleted = 0;
        for (String file : files) {
            try {
                if (Files.deleteIfExists(Paths.get(file))) {
                    deleted++;
                }
            } catch (IOException e) {
                System.err.println("  删除文件失败: " + file);
            }
        }

        System.out.println("✓ 清理完成 (删除了 " + deleted + " 个文件)\n");
    }
}