package org.example.demo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class DemoAction extends AnAction {

    private String appId = "";

    private String appKey = "";

    private String annotate = "";

    /**
     * 分隔符
     */
    private static final String separator = "#####";

    @Override
    public void actionPerformed(AnActionEvent event) {
        MySettingsState mySettings = MySettingsState.getInstance().getState();
        if (mySettings.getAppId() == null || mySettings.getAppId().length() == 0
                || mySettings.getAppKey() == null || mySettings.getAppKey().length() == 0
                || mySettings.getAnnotate() == null || mySettings.getAnnotate().length() == 0) {
            throw new RuntimeException("请先配置appId和appKey以及包名");
        }
        appId = mySettings.getAppId();
        appKey = mySettings.getAppKey();
        annotate = mySettings.getAnnotate();
        // 获取当前编辑的文件, 可以进而获取 PsiClass, PsiField 对象
        PsiFile psiFile = DataKeys.PSI_FILE.getData(event.getDataContext());
        PsiJavaFile psiJavaFile;
        if (psiFile instanceof PsiJavaFile) {
            psiJavaFile = (PsiJavaFile) psiFile;
        } else {
            throw new RuntimeException("当前文件不是Java文件");
        }
        PsiClass psiClass = psiJavaFile.getClasses()[0];

        if (psiClass != null) {
            addAnnotation(psiClass, psiJavaFile);
        }
    }

    public void addAnnotation(PsiClass psiClass, PsiJavaFile psiJavaFile) {

        Project project = psiClass.getProject();
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

        StringBuilder wordStr = new StringBuilder();
        // 遍历每个类中的字段
        PsiField[] fields = psiClass.getFields();
        for (PsiField field : fields) {
            // 检查是否为 final 和 static，是则跳过
            boolean isFinal = field.hasModifierProperty(PsiModifier.FINAL);
            boolean isStatic = field.hasModifierProperty(PsiModifier.STATIC);
            if (isFinal || isStatic) {
                continue;
            }
            // 获取字段名
            String fieldName = field.getName();
            if (wordStr.length() > 0) {
                // 加一个隔离符区分开，方便百度翻译一次性查询 PS：通用翻译接口需要间隔一段时间才能继续请求，否则会报错
                wordStr.append(separator).append(replaceLower(fieldName));
            } else {
                wordStr.append(replaceLower(fieldName));
            }

        }
        String[] wordList = new String[]{};
        if (wordStr.length() > 0) {
            String translateWords = translateWords(wordStr.toString());
            if (translateWords == null) {
                throw new RuntimeException("翻译失败");
            }
            // 把之前的隔离符还原
            wordList = translateWords.split(separator);
        }
        // 使用 WriteCommandAction 保存更改
        String[] finalWordList = wordList;
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            Document document = documentManager.getDocument(psiJavaFile);
            int wordI = 0;
            for (int i = 0; i < fields.length; i++) {
                PsiField field = fields[i];
                // 检查是否为 final 和 static，是则跳过
                boolean isFinal = field.hasModifierProperty(PsiModifier.FINAL);
                boolean isStatic = field.hasModifierProperty(PsiModifier.STATIC);
                if (isFinal || isStatic) {
                    continue;
                }
                // 创建注解
                String annotationText = String.format("@Demo(value = \"" + field.getName() + "\", description =\"%s\")", finalWordList[wordI]);
                PsiAnnotation annotation = factory.createAnnotationFromText(annotationText, psiClass);
                wordI++;
                if (isSerialVersionUID(field.getContext())) {
                    continue;
                }
                // 获取属性声明的可能存在的firstChild PS：属性字段上面有可能会存在注释说明，所以需要特殊处理
                PsiElement firstChild = field.getFirstChild();
                try {
                    // 如果是注释说明
                    if (firstChild instanceof PsiDocComment) {
                        // 获取定位：注释说明的起始位置 + 注释说明的长度
                        int childTextOffset = firstChild.getTextOffset();
                        int childTextLength = firstChild.getTextLength();
                        // 如果字段上面有注释，则特殊处理，直接用Document插入注解
                        document.insertString(childTextOffset + childTextLength, "\n\t" + annotationText);
                        // 提交文档，并解锁（解锁后，其他线程才能修改）
                        documentManager.commitDocument(document);
                        documentManager.doPostponedOperationsAndUnblockDocument(document);
                    } else {
                        psiClass.addBefore(annotation, field);
                    }
                } catch (Throwable e) {
                    System.out.println(e.getMessage());
                }
            }
            // 获取当前全部的导入语句
            PsiImportList importList = psiJavaFile.getImportList();
            // 添加导入语句，这个createImportStatementOnDemand其实有个问题，会把包名自动加上.*，不过这就是个例子，为了方便引出后面的使用，就先这么写了
            if (importList != null && importList.findOnDemandImportStatement(annotate) == null) {
                importList.add(factory.createImportStatementOnDemand(annotate));
            }
        });
        // 处理导入注释import结尾会自动附带.*的问题
        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            Document document = documentManager.getDocument(psiJavaFile);
            PsiImportList psiImportList = psiJavaFile.getImportList();
            // 返回列表中包含的非静态 import 语句
            PsiImportStatement[] importStatements = psiImportList.getImportStatements();
            for (PsiImportStatement importStatement : importStatements) {
                if (importStatement.getText().contains(annotate + ".*")) {
                    // 获取当前java文件转化的文本内容
                    String text = document.getText();
                    String all = text.replaceAll(annotate + "\\.\\*", annotate);
                    // 直接对文本进行替换，然后提交文档，即可修改当前java文件
                    document.setText(all);
                    documentManager.commitDocument(document);
                    documentManager.doPostponedOperationsAndUnblockDocument(document);
                }
            }
        });
    }

    private static boolean isSerialVersionUID(PsiElement element) {
        return element instanceof PsiField
                && "serialVersionUID".equals(((PsiField) element).getName())
                && ((PsiField) element).hasModifierProperty(PsiModifier.STATIC)
                && ((PsiField) element).hasModifierProperty(PsiModifier.FINAL)
                && ((PsiField) element).getType().equalsToText("long");
    }

    private String replaceLower(String text) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char currentChar = text.charAt(i);
            // 检查当前字符是否是大写字母，并且前一个字符不是大写字母或者已经是空格
            if (Character.isUpperCase(currentChar) &&
                    (i == 0 || !Character.isUpperCase(text.charAt(i - 1)))) {
                result.append(' ').append(Character.toLowerCase(currentChar));
            } else {
                result.append(currentChar);
            }
        }
        return result.toString();
    }

    private String translateWords(String word) {
        try {
            String appKeySettings = appKey;

            String appIdSettings = appId;

            String encodedText = URLEncoder.encode(word, StandardCharsets.UTF_8);

            String url = "http://api.fanyi.baidu.com/api/trans/vip/translate?q=" + encodedText + "&from=en&to=zh&appid=" + appIdSettings + "&salt=1435660288&sign=" + md5(appIdSettings + word + "1435660288" + appKeySettings);

            HttpClient client = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            JSONObject jsonObject = JSONObject.parseObject(result.toString());
            JSONArray transResultArray = jsonObject.getJSONArray("trans_result");
            if (transResultArray == null) {
                return word;
            }
            if (transResultArray.size() == 0) {
                return word;
            }
            JSONObject jsonObject1 = transResultArray.getJSONObject(0);
            return jsonObject1.getString("dst");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String md5(String input) {

        return DigestUtils.md5Hex(input);
    }
}
