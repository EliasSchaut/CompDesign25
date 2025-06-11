package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.lexer.tokens.Identifier;
import edu.kit.kastel.vads.compiler.lexer.tokens.Keyword;
import edu.kit.kastel.vads.compiler.lexer.tokens.NumberLiteral;
import edu.kit.kastel.vads.compiler.lexer.tokens.Operator;
import edu.kit.kastel.vads.compiler.lexer.tokens.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.lexer.tokens.Separator;
import edu.kit.kastel.vads.compiler.lexer.tokens.Separator.SeparatorType;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.tokens.Token;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ControlTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.NegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final TokenSource tokenSource;
    private boolean hasMainMethod = false;

    public Parser(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
    }

    public ProgramTree parseProgram() {
        ProgramTree programTree = new ProgramTree(List.of(parseFunction()));
        if (this.tokenSource.hasMore()) {
            throw new ParseException("expected end of input but got " + this.tokenSource.peek());
        }
        if (!hasMainMethod) {
            throw new ParseException("expected a main method");
        }
        return programTree;
    }

    private FunctionTree parseFunction() {
        Keyword returnType = this.tokenSource.expectKeyword(Keyword.KeywordType.INT);
        Identifier identifier = this.tokenSource.expectIdentifier();
        if (!hasMainMethod && identifier.asString().equals("main")) hasMainMethod = true;
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        BlockTree body = parseBlock();
        return new FunctionTree(
            new TypeTree(BasicType.INT, returnType.span()),
            name(identifier),
            body
        );
    }

    private BlockTree parseBlock() {
        Separator bodyOpen = this.tokenSource.expectSeparator(SeparatorType.BRACE_OPEN);
        List<StatementTree> statements = new ArrayList<>();
        while (!(this.tokenSource.peek() instanceof Separator sep && sep.type() == SeparatorType.BRACE_CLOSE)) {
            statements.add(parseStatement());
        }
        Separator bodyClose = this.tokenSource.expectSeparator(SeparatorType.BRACE_CLOSE);
        return new BlockTree(statements, bodyOpen.span().merge(bodyClose.span()));
    }

    private TypeTree parseType() {
        Token nextToken = this.tokenSource.peek();
        if (nextToken.isKeyword(Keyword.KeywordType.INT)) {
            return new TypeTree(BasicType.INT, nextToken.span());
        } else if (nextToken.isKeyword(Keyword.KeywordType.BOOL)) {
            return new TypeTree(BasicType.BOOL, nextToken.span());
        } else {
            throw new ParseException("expected type but got " + nextToken);
        }
    }

    private StatementTree parseDeclaration() {
        TypeTree type = parseType();
        Identifier ident = this.tokenSource.expectIdentifier();
        ExpressionTree expr = null;
        if (this.tokenSource.peek().isOperator(OperatorType.ASSIGN)) {
            this.tokenSource.expectOperator(OperatorType.ASSIGN);
            expr = parseExpression();
        }
        return new DeclarationTree(type, name(ident), expr);
    }

    private StatementTree parseStatement() {
        StatementTree statement;
        Token nextToken = this.tokenSource.peek();
        if (isType(nextToken)) {
            statement = parseDeclaration();
        } else if (isControl(nextToken)) {
            statement = parseControl();
        } else {
            statement = parseSimple();
        }
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        return statement;
    }

    private StatementTree parseSimple() {
        LValueTree lValue = parseLValue();
        Operator assignmentOperator = parseAssignmentOperator();
        ExpressionTree expression = parseExpression();
        return new AssignmentTree(lValue, assignmentOperator, expression);
    }

    private LValueTree parseLValue() {
        if (this.tokenSource.peek().isSeparator(SeparatorType.PAREN_OPEN)) {
            this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
            LValueTree inner = parseLValue();
            this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
            return inner;
        }
        Identifier identifier = this.tokenSource.expectIdentifier();
        return new LValueIdentTree(name(identifier));
    }

    private ControlTree parseControl() {
        ControlTree control;
        Token nextToken = this.tokenSource.peek();
        if (nextToken.isKeyword(Keyword.KeywordType.IF)) {
            control = parseIf();
        } else if (nextToken.isKeyword(Keyword.KeywordType.WHILE)) {
            control = parseWhile();
        } else if (nextToken.isKeyword(Keyword.KeywordType.FOR)) {
            control = parseFor();
        } else if (nextToken.isKeyword(Keyword.KeywordType.CONTINUE)) {
            control = parseContinue();
        } else if (nextToken.isKeyword(Keyword.KeywordType.BREAK)) {
            control = parseBreak();
        } else if (nextToken.isKeyword(Keyword.KeywordType.RETURN)) {
            control = parseReturn();
        } else {
            throw new ParseException("expected control statement but got " + nextToken);
        }

        return control;
    }

    private IfTree parseIf() {
        this.tokenSource.expectKeyword(Keyword.KeywordType.IF);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree condition = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        BlockTree thenBlock = parseBlock();
        BlockTree elseBlock = null;
        if (this.tokenSource.peek().isKeyword(Keyword.KeywordType.ELSE)) {
            this.tokenSource.consume();
            elseBlock = parseBlock();
        }
        return new IfTree(condition, thenBlock, elseBlock);
    }

    private ControlTree parseWhile() {
        this.tokenSource.expectKeyword(Keyword.KeywordType.WHILE);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree condition = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        BlockTree body = parseBlock();
        return new WhileTree(condition, body);
    }

    private ControlTree parseFor() {
        this.tokenSource.expectKeyword(Keyword.KeywordType.FOR);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        StatementTree init = parseStatement();
        ExpressionTree condition = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        StatementTree update = parseStatement();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        BlockTree body = parseBlock();
        return new ForTree(init, condition, update, body);
    }

    private ControlTree parseContinue() {
        Keyword cont = this.tokenSource.expectKeyword(Keyword.KeywordType.CONTINUE);
        return new ContinueTree(cont.span());
    }

    private ControlTree parseBreak() {
        Keyword brk = this.tokenSource.expectKeyword(Keyword.KeywordType.BREAK);
        return new BreakTree(brk.span());
    }

    private ReturnTree parseReturn() {
        Keyword ret = this.tokenSource.expectKeyword(Keyword.KeywordType.RETURN);
        ExpressionTree expression = parseExpression();
        return new ReturnTree(expression, ret.span().start());
    }

    private ExpressionTree parseExpression() {
        ExpressionTree lhs = parseTerm();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _)
                && (type == OperatorType.PLUS || type == OperatorType.MINUS)) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseTerm(), type);
            } else {
                return lhs;
            }
        }
    }

    private Operator parseAssignmentOperator() {
        if (this.tokenSource.peek() instanceof Operator op) {
            return switch (op.type()) {
                case ASSIGN, ASSIGN_DIV, ASSIGN_MINUS, ASSIGN_MOD, ASSIGN_MUL, ASSIGN_PLUS -> {
                    this.tokenSource.consume();
                    yield op;
                }
                default -> throw new ParseException("expected assignment but got " + op.type());
            };
        }
        throw new ParseException("expected assignment but got " + this.tokenSource.peek());
    }

    private ExpressionTree parseTerm() {
        ExpressionTree lhs = parseFactor();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _)
                && (type == OperatorType.MUL || type == OperatorType.DIV || type == OperatorType.MOD)) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseFactor(), type);
            } else {
                return lhs;
            }
        }
    }

    private ExpressionTree parseFactor() {
        return switch (this.tokenSource.peek()) {
            case Separator(var type, _) when type == SeparatorType.PAREN_OPEN -> {
                this.tokenSource.consume();
                ExpressionTree expression = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                yield expression;
            }
            case Operator(var type, _) when type == OperatorType.MINUS -> {
                Span span = this.tokenSource.consume().span();
                yield new NegateTree(parseFactor(), span);
            }
            case Identifier ident -> {
                this.tokenSource.consume();
                yield new IdentExpressionTree(name(ident));
            }
            case NumberLiteral(String value, int base, Span span) -> {
                this.tokenSource.consume();
                yield new LiteralTree(value, base, span);
            }
            case Token t -> throw new ParseException("invalid factor " + t);
        };
    }

    private static NameTree name(Identifier ident) {
        return new NameTree(Name.forIdentifier(ident), ident.span());
    }

    private static boolean isType(Token token) {
        return token.isKeyword(Keyword.KeywordType.INT) || token.isKeyword(Keyword.KeywordType.BOOL);
    }

    private static boolean isControl(Token token) {
        return token.isKeyword(Keyword.KeywordType.IF)
            || token.isKeyword(Keyword.KeywordType.WHILE)
            || token.isKeyword(Keyword.KeywordType.FOR)
            || token.isKeyword(Keyword.KeywordType.CONTINUE)
            || token.isKeyword(Keyword.KeywordType.BREAK)
            || token.isKeyword(Keyword.KeywordType.RETURN);
    }
}
