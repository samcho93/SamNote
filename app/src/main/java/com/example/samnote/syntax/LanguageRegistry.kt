package com.example.samnote.syntax

/**
 * 언어 정의 인터페이스
 */
interface LanguageDefinition {
    val rules: List<SyntaxRule>
}

/**
 * 파일 확장자 → 언어 정의 매핑
 */
object LanguageRegistry {

    fun forExtension(extension: String): LanguageDefinition? {
        return when (extension.lowercase()) {
            "c", "h" -> CLang
            "cpp", "hpp", "cc", "cxx" -> CLang
            "java" -> JavaLang
            "kt", "kts" -> KotlinLang
            "py" -> PythonLang
            "js", "jsx", "ts", "tsx" -> JavaScriptLang
            "md", "markdown" -> MarkdownLang
            else -> null
        }
    }

    fun isMarkdown(extension: String): Boolean =
        extension.lowercase() in setOf("md", "markdown")

    fun languageName(extension: String): String {
        return when (extension.lowercase()) {
            "c", "h" -> "C"
            "cpp", "hpp", "cc", "cxx" -> "C++"
            "java" -> "Java"
            "kt", "kts" -> "Kotlin"
            "py" -> "Python"
            "js", "jsx" -> "JavaScript"
            "ts", "tsx" -> "TypeScript"
            "md", "markdown" -> "Markdown"
            "txt" -> "Text"
            "json" -> "JSON"
            "xml" -> "XML"
            "html", "htm" -> "HTML"
            "css", "scss" -> "CSS"
            "yaml", "yml" -> "YAML"
            "sql" -> "SQL"
            "sh", "bash" -> "Shell"
            "rs" -> "Rust"
            "go" -> "Go"
            "swift" -> "Swift"
            "dart" -> "Dart"
            "rb" -> "Ruby"
            "php" -> "PHP"
            else -> extension.uppercase()
        }
    }
}

// ── C / C++ ───────────────────────────────────────────
object CLang : LanguageDefinition {
    override val rules = listOf(
        // 블록 주석
        SyntaxRule(Regex("/\\*[\\s\\S]*?\\*/"), TokenType.COMMENT),
        // 라인 주석
        SyntaxRule(Regex("//[^\n]*"), TokenType.COMMENT),
        // 전처리기 지시문
        SyntaxRule(Regex("^\\s*#\\s*\\w+[^\n]*", RegexOption.MULTILINE), TokenType.ANNOTATION),
        // 문자열
        SyntaxRule(Regex("\"(?:[^\"\\\\]|\\\\.)*\""), TokenType.STRING),
        // 문자 리터럴
        SyntaxRule(Regex("'(?:[^'\\\\]|\\\\.)'"), TokenType.STRING),
        // 숫자
        SyntaxRule(Regex("\\b(?:0[xX][0-9a-fA-F]+|0[bB][01]+|[0-9]+\\.?[0-9]*[fFlL]?)\\b"), TokenType.NUMBER),
        // 타입 키워드
        SyntaxRule(
            Regex("\\b(?:int|char|float|double|long|short|unsigned|signed|void|bool|size_t|FILE)\\b"),
            TokenType.TYPE
        ),
        // 키워드
        SyntaxRule(
            Regex("\\b(?:auto|break|case|const|continue|default|do|else|enum|extern|for|goto|if|inline|register|restrict|return|sizeof|static|struct|switch|typedef|union|volatile|while|NULL|true|false)\\b"),
            TokenType.KEYWORD
        ),
    )
}

// ── Java ──────────────────────────────────────────────
object JavaLang : LanguageDefinition {
    override val rules = listOf(
        SyntaxRule(Regex("/\\*[\\s\\S]*?\\*/"), TokenType.COMMENT),
        SyntaxRule(Regex("//[^\n]*"), TokenType.COMMENT),
        SyntaxRule(Regex("\"(?:[^\"\\\\]|\\\\.)*\""), TokenType.STRING),
        SyntaxRule(Regex("'(?:[^'\\\\]|\\\\.)'"), TokenType.STRING),
        SyntaxRule(Regex("@\\w+"), TokenType.ANNOTATION),
        SyntaxRule(Regex("\\b(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|[0-9][0-9_]*\\.?[0-9_]*[fFdDlL]?)\\b"), TokenType.NUMBER),
        SyntaxRule(
            Regex("\\b(?:String|Integer|Boolean|Long|Double|Float|Object|List|Map|Set|Array|Void|Class|Exception)\\b"),
            TokenType.TYPE
        ),
        SyntaxRule(
            Regex("\\b(?:abstract|assert|break|case|catch|class|const|continue|default|do|else|enum|extends|final|finally|for|goto|if|implements|import|instanceof|interface|native|new|package|private|protected|public|return|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|volatile|while|true|false|null|void|int|char|float|double|long|short|byte|boolean)\\b"),
            TokenType.KEYWORD
        ),
    )
}

// ── Kotlin ────────────────────────────────────────────
object KotlinLang : LanguageDefinition {
    override val rules = listOf(
        SyntaxRule(Regex("/\\*[\\s\\S]*?\\*/"), TokenType.COMMENT),
        SyntaxRule(Regex("//[^\n]*"), TokenType.COMMENT),
        SyntaxRule(Regex("\"\"\"[\\s\\S]*?\"\"\""), TokenType.STRING),
        SyntaxRule(Regex("\"(?:[^\"\\\\]|\\\\.)*\""), TokenType.STRING),
        SyntaxRule(Regex("'(?:[^'\\\\]|\\\\.)'"), TokenType.STRING),
        SyntaxRule(Regex("@\\w+"), TokenType.ANNOTATION),
        SyntaxRule(Regex("\\b(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|[0-9][0-9_]*\\.?[0-9_]*[fFdDlL]?)\\b"), TokenType.NUMBER),
        SyntaxRule(
            Regex("\\b(?:String|Int|Long|Double|Float|Boolean|Char|Unit|Any|Nothing|List|Map|Set|Array|Pair|Triple)\\b"),
            TokenType.TYPE
        ),
        SyntaxRule(
            Regex("\\b(?:abstract|actual|annotation|as|break|by|catch|class|companion|const|constructor|continue|crossinline|data|do|else|enum|expect|external|false|final|finally|for|fun|get|if|import|in|infix|init|inline|inner|interface|internal|is|it|lateinit|noinline|null|object|open|operator|out|override|package|private|protected|public|reified|return|sealed|set|super|suspend|tailrec|this|throw|true|try|typealias|val|var|vararg|when|where|while)\\b"),
            TokenType.KEYWORD
        ),
    )
}

// ── Python ────────────────────────────────────────────
object PythonLang : LanguageDefinition {
    override val rules = listOf(
        // 삼중 따옴표 문자열 (주석/문자열 겸용)
        SyntaxRule(Regex("\"\"\"[\\s\\S]*?\"\"\""), TokenType.STRING),
        SyntaxRule(Regex("'''[\\s\\S]*?'''"), TokenType.STRING),
        // 라인 주석
        SyntaxRule(Regex("#[^\n]*"), TokenType.COMMENT),
        // 문자열
        SyntaxRule(Regex("\"(?:[^\"\\\\]|\\\\.)*\""), TokenType.STRING),
        SyntaxRule(Regex("'(?:[^'\\\\]|\\\\.)*'"), TokenType.STRING),
        // 데코레이터
        SyntaxRule(Regex("@\\w+"), TokenType.ANNOTATION),
        // 숫자
        SyntaxRule(Regex("\\b(?:0[xX][0-9a-fA-F_]+|0[oO][0-7_]+|0[bB][01_]+|[0-9][0-9_]*\\.?[0-9_]*[jJ]?)\\b"), TokenType.NUMBER),
        // 내장 타입
        SyntaxRule(
            Regex("\\b(?:int|float|str|bool|list|dict|tuple|set|bytes|type|object|None)\\b"),
            TokenType.TYPE
        ),
        // 키워드
        SyntaxRule(
            Regex("\\b(?:False|True|None|and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield|self|cls|print)\\b"),
            TokenType.KEYWORD
        ),
    )
}

// ── JavaScript / TypeScript ───────────────────────────
object JavaScriptLang : LanguageDefinition {
    override val rules = listOf(
        SyntaxRule(Regex("/\\*[\\s\\S]*?\\*/"), TokenType.COMMENT),
        SyntaxRule(Regex("//[^\n]*"), TokenType.COMMENT),
        // 템플릿 리터럴
        SyntaxRule(Regex("`(?:[^`\\\\]|\\\\.)*`"), TokenType.STRING),
        SyntaxRule(Regex("\"(?:[^\"\\\\]|\\\\.)*\""), TokenType.STRING),
        SyntaxRule(Regex("'(?:[^'\\\\]|\\\\.)*'"), TokenType.STRING),
        SyntaxRule(Regex("\\b(?:0[xX][0-9a-fA-F_]+|0[oO][0-7_]+|0[bB][01_]+|[0-9][0-9_]*\\.?[0-9_]*[n]?)\\b"), TokenType.NUMBER),
        // TypeScript 타입
        SyntaxRule(
            Regex("\\b(?:string|number|boolean|any|void|never|unknown|undefined|null|Array|Promise|Record|Partial|Required|Readonly)\\b"),
            TokenType.TYPE
        ),
        SyntaxRule(
            Regex("\\b(?:abstract|arguments|async|await|break|case|catch|class|const|continue|debugger|default|delete|do|else|enum|export|extends|false|finally|for|from|function|get|if|implements|import|in|instanceof|interface|let|new|null|of|package|private|protected|public|return|set|static|super|switch|this|throw|true|try|type|typeof|undefined|var|void|while|with|yield)\\b"),
            TokenType.KEYWORD
        ),
    )
}

// ── Markdown (편집 모드 구문 강조) ────────────────────
object MarkdownLang : LanguageDefinition {
    override val rules = listOf(
        // 코드 블록 (```)
        SyntaxRule(Regex("```[\\s\\S]*?```"), TokenType.MD_CODE),
        // 인라인 코드
        SyntaxRule(Regex("`[^`\n]+`"), TokenType.MD_CODE),
        // 제목 (# ~ ######)
        SyntaxRule(Regex("^#{1,6}\\s+.*$", RegexOption.MULTILINE), TokenType.MD_HEADING),
        // 볼드 **text** 또는 __text__
        SyntaxRule(Regex("\\*\\*[^*]+\\*\\*|__[^_]+__"), TokenType.MD_BOLD),
        // 이탈릭 *text* 또는 _text_
        SyntaxRule(Regex("\\*[^*\n]+\\*"), TokenType.MD_ITALIC),
        // 링크 [text](url)
        SyntaxRule(Regex("\\[([^\\]]*)]\\([^)]*\\)"), TokenType.MD_LINK),
        // 인용문
        SyntaxRule(Regex("^>\\s+.*$", RegexOption.MULTILINE), TokenType.MD_BLOCKQUOTE),
        // 리스트 마커
        SyntaxRule(Regex("^[\\t ]*[-*+]\\s", RegexOption.MULTILINE), TokenType.MD_LIST_MARKER),
        SyntaxRule(Regex("^\\s*\\d+\\.\\s", RegexOption.MULTILINE), TokenType.MD_LIST_MARKER),
    )
}
