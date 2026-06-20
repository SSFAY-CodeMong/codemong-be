package com.codemong.be.rag.service;

import com.codemong.be.github.service.GithubService;
import com.codemong.be.repository.entity.GithubRepository;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RAGService {
    private final VectorStore vectorStore;
    private final GithubService githubService;

    @Transactional
    public void save(Long userId, Long repositoryId, Map<String, String> contents) {
        String version = UUID.randomUUID().toString();

        if(contents.isEmpty()){
            // 합의로 step 넣을지 말지 결정 필요 -> save 에도 넣을지 말지 고민해야함
            // 만약에 넣게된다? -> aiService에서 전달안되던 step 전달/
            // githubService에서 레포지토리의 최신 브랜치네임을 가져오는것이 아닌 레포와 step으로 검사 ->
            // 그러기 위해서는 step이 Long이어야함
            contents = githubService.getBranchContents(repositoryId, 1L, userId);
        }

        // 저장 로직
        List<Document> documents = new ArrayList<>();

        for (Map.Entry<String, String> entry : contents.entrySet()) {
            String filePath = entry.getKey();
            String content = entry.getValue();

            if (isJavaFile(filePath)) {
                documents.addAll(splitJavaFile(filePath, content, userId, repositoryId, version));
            } else {
                documents.add(createDocument(content, userId, repositoryId, filePath, "", "", "file", version));
            }
        }


        FilterExpressionBuilder b = new FilterExpressionBuilder();

        if (!documents.isEmpty()) {
            try {
                vectorStore.add(documents);
                vectorStore.delete(
                        b.and(
                                b.and(
                                    b.eq("userId", userId),
                                    b.eq("repositoryId", repositoryId)
                                ),
                                b.ne("version", version)
                        ).build()
                );
            } catch (Exception e) {
                try {
                    vectorStore.delete(
                            b.and(
                                    b.and(
                                            b.eq("userId", userId),
                                            b.eq("repositoryId", repositoryId)
                                    ),
                                    b.eq("version", version)
                            ).build()
                    );
                }catch(Exception e1){
                    e.addSuppressed(e1);
                }
                throw new RuntimeException(e);
            }
        }

        if(documents.isEmpty()){
            try {
                vectorStore.delete(
                        b.and(
                              b.eq("userId", userId),
                              b.eq("repositoryId", repositoryId)
                        ).build()
                );
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    public String searchSimilarCode(String question, Long userId, Long repositoryId){
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(5)
                .filterExpression(b.and(
                        b.eq("userId", userId),
                        b.eq("repositoryId", repositoryId)
                ).build())
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        return documents.stream()
                .map(this::formatDocumentForPrompt)
                .collect(Collectors.joining(("\n\n---\n\n")));
    }

    private String formatDocumentForPrompt(Document document) {
        Map<String, Object> metadata = document.getMetadata();

        String filePath = metadata.get("filePath") == null ? "N/A" : metadata.get("filePath").toString();
        String className = metadata.get("className") == null ? "N/A" : metadata.get("className").toString();
        String methodName = metadata.get("methodName") == null ? "N/A" : metadata.get("methodName").toString();
        String chunkType = metadata.get("chunkType") == null ? "N/A" : metadata.get("chunkType").toString();

        return """
              File: %s
              ChunkType: %s
              Class: %s
              Method: %s

              ```%s
              %s
              ```
              """.formatted(
                filePath,
                chunkType,
                className,
                methodName,
                resolveCodeFenceLanguage(filePath),
                document.getText()
        );

    }

    private String resolveCodeFenceLanguage(String filePath) {
        if (filePath == null) {
            return "";
        }

        if (filePath.endsWith(".java")) {
            return "java";
        }
        if (filePath.endsWith(".json")) {
            return "json";
        }
        if (filePath.endsWith(".xml")) {
            return "xml";
        }
        if (filePath.endsWith(".yml") || filePath.endsWith(".yaml")) {
            return "yaml";
        }
        if (filePath.endsWith(".properties")) {
            return "properties";
        }
        if (filePath.endsWith(".gradle")) {
            return "groovy";
        }
        if (filePath.endsWith(".js") || filePath.endsWith(".jsx")) {
            return "javascript";
        }
        if (filePath.endsWith(".ts") || filePath.endsWith(".tsx")) {
            return "typescript";
        }
        if (filePath.endsWith(".html")) {
            return "html";
        }
        if (filePath.endsWith(".css")) {
            return "css";
        }

        return "";
    }


    private List<Document> splitJavaFile(String filePath, String content, Long userId, Long repositoryId, String version) {
        List<Document> documents = new ArrayList<>();

        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(content);

            documents.addAll(splitTypes(filePath, content, compilationUnit, userId, repositoryId, version));
            documents.addAll(splitMethods(filePath, content, compilationUnit, userId, repositoryId, version));
            documents.addAll(splitConstructors(filePath, content, compilationUnit, userId, repositoryId, version));

            if (documents.isEmpty()) {
                documents.add(createDocument(content, userId, repositoryId, filePath, "", "", "file", version));
            }

            return documents;
        } catch (ParseProblemException e) {
            return List.of(createDocument(content, userId, repositoryId, filePath, "", "", "file", version));
        }
    }

    private List<Document> splitTypes(
            String filePath,
            String content,
            CompilationUnit compilationUnit,
            Long userId,
            Long repositoryId,
            String version
    ) {
        List<Document> documents = new ArrayList<>();

        for (TypeDeclaration<?> type : compilationUnit.findAll(TypeDeclaration.class)) {
            extractSource(content, type).ifPresent(typeText -> documents.add(createDocument(
                    typeText,
                    userId,
                    repositoryId,
                    filePath,
                    type.getNameAsString(),
                    "",
                    "class",
                    version
            )));
        }

        return documents;
    }

    private List<Document> splitMethods(
            String filePath,
            String content,
            CompilationUnit compilationUnit,
            Long userId,
            Long repositoryId,
            String version
    ) {
        List<Document> documents = new ArrayList<>();

        for (MethodDeclaration method : compilationUnit.findAll(MethodDeclaration.class)) {
            extractSource(content, method).ifPresent(methodText -> documents.add(createDocument(
                    methodText,
                    userId,
                    repositoryId,
                    filePath,
                    findOwnerTypeName(method),
                    method.getNameAsString(),
                    "method",
                    version
            )));
        }

        return documents;
    }

    private List<Document> splitConstructors(
            String filePath,
            String content,
            CompilationUnit compilationUnit,
            Long userId,
            Long repositoryId,
            String version
    ) {
        List<Document> documents = new ArrayList<>();

        for (ConstructorDeclaration constructor : compilationUnit.findAll(ConstructorDeclaration.class)) {
            extractSource(content, constructor).ifPresent(constructorText -> documents.add(createDocument(
                    constructorText,
                    userId,
                    repositoryId,
                    filePath,
                    findOwnerTypeName(constructor),
                    constructor.getNameAsString(),
                    "constructor",
                    version
            )));
        }

        return documents;
    }

    private Document createDocument(
            String text,
            Long userId,
            Long repositoryId,
            String filePath,
            String className,
            String methodName,
            String chunkType,
            String version
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userId);
        metadata.put("repositoryId", repositoryId);
        metadata.put("filePath", filePath);
        metadata.put("className", className);
        metadata.put("methodName", methodName);
        metadata.put("chunkType", chunkType);
        metadata.put("version", version);

        return new Document(text, metadata);
    }

    private boolean isJavaFile(String filePath) {
        return filePath != null && filePath.endsWith(".java");
    }

    private String findOwnerTypeName(Node node) {
        return node.findAncestor(TypeDeclaration.class)
                .map(TypeDeclaration::getNameAsString)
                .orElse("");
    }

    private Optional<String> extractSource(String content, Node node) {
        return node.getRange()
                .map(range -> substringByRange(content, range));
    }

    private String substringByRange(String content, Range range) {
        int beginIndex = positionToIndex(content, range.begin);
        int endIndex = Math.min(positionToIndex(content, range.end) + 1, content.length());

        if (beginIndex >= endIndex) {
            return "";
        }

        return content.substring(beginIndex, endIndex);
    }

    private int positionToIndex(String content, Position position) {
        int currentLine = 1;
        int currentColumn = 1;

        for (int i = 0; i < content.length(); i++) {
            if (currentLine == position.line && currentColumn == position.column) {
                return i;
            }

            if (content.charAt(i) == '\n') {
                currentLine++;
                currentColumn = 1;
            } else {
                currentColumn++;
            }
        }

        return content.length();
    }
}
