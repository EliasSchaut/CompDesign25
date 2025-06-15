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
import edu.kit.kastel.vads.compiler.parser.ast.expression.BooleanTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.UnaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ControlTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.lvalue.LValueTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.expression.operation.TernaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.statement.control.WhileTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

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
        if (!hasMainMethod && identifier.asString().equals("main")) {
            hasMainMethod = true;
        }
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
        while (!(this.tokenSource.peek() instanceof Separator sep &&
            sep.type() == SeparatorType.BRACE_CLOSE)) {
            statements.add(parseStatement());
        }
        Separator bodyClose = this.tokenSource.expectSeparator(SeparatorType.BRACE_CLOSE);
        return new BlockTree(statements, bodyOpen.span().merge(bodyClose.span()));
    }

    private TypeTree parseType() {
        Token nextToken = this.tokenSource.peek();
        if (nextToken.isKeyword(Keyword.KeywordType.INT)) {
            this.tokenSource.consume();
            return new TypeTree(BasicType.INT, nextToken.span());
        } else if (nextToken.isKeyword(Keyword.KeywordType.BOOL)) {
            this.tokenSource.consume();
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
        if (isControl(nextToken)) {
            statement = parseControl();
        } else if (nextToken.isSeparator(SeparatorType.BRACE_OPEN)) {
            statement = parseBlock();
        } else {
            statement = parseSimple();
            this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        }
        return statement;
    }

    private StatementTree parseSimple() {
        Token nextToken = this.tokenSource.peek();
        if (isType(nextToken)) {
            return parseDeclaration();
        } else {
            LValueTree lValue = parseLValue();
            Operator assignmentOperator = parseAssignmentOperator();
            ExpressionTree expression = parseExpression();
            return new AssignmentTree(lValue, assignmentOperator, expression);
        }
    }

    private @Nullable StatementTree parseSimpleOptional() {
        Token nextToken = this.tokenSource.peek();
        if (nextToken.isSeparator(SeparatorType.SEMICOLON) // via for loop
            || nextToken.isSeparator(SeparatorType.PAREN_CLOSE) // via for loop
        ) {
            return null;
        } else {
            return parseSimple();
        }
    }

    private Operator parseAssignmentOperator() {
        if (this.tokenSource.peek() instanceof Operator op) {
            return switch (op.type()) {
                case ASSIGN, ASSIGN_DIV, ASSIGN_MINUS, ASSIGN_MOD, ASSIGN_MUL, ASSIGN_PLUS, ASSIGN_AND, ASSIGN_OR, ASSIGN_XOR, ASSIGN_SHIFT_LEFT, ASSIGN_SHIFT_RIGHT -> {
                    this.tokenSource.consume();
                    yield op;
                }
                default -> throw new ParseException("expected assignment but got " + op.type());
            };
        }
        throw new ParseException("expected assignment but got " + this.tokenSource.peek());
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
            this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        } else if (nextToken.isKeyword(Keyword.KeywordType.BREAK)) {
            control = parseBreak();
            this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        } else if (nextToken.isKeyword(Keyword.KeywordType.RETURN)) {
            control = parseReturn();
            this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        } else {
            throw new ParseException("expected control statement but got " + nextToken);
        }

        return control;
    }

    private IfTree parseIf() {
        Keyword ifKeyword = this.tokenSource.expectKeyword(Keyword.KeywordType.IF);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree condition = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        StatementTree thenBlock = parseStatement();
        StatementTree elseBlock = null;
        if (this.tokenSource.peek().isKeyword(Keyword.KeywordType.ELSE)) {
            this.tokenSource.consume();
            elseBlock = parseStatement();
        }
        return new IfTree(ifKeyword, condition, thenBlock, elseBlock);
    }

    private ControlTree parseWhile() {
        Keyword whileKeyword = this.tokenSource.expectKeyword(Keyword.KeywordType.WHILE);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree condition = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        StatementTree body = parseStatement();
        return new WhileTree(whileKeyword, condition, body);
    }

    private ControlTree parseFor() {
        Keyword forKeyword = this.tokenSource.expectKeyword(Keyword.KeywordType.FOR);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        StatementTree init = parseSimpleOptional();
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        ExpressionTree condition = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        StatementTree update = parseSimpleOptional();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        StatementTree body = parseStatement();
        return new ForTree(forKeyword, init, condition, update, body);
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
        // Parse via precedence climbing
        return parsePrecedence(0);
    }

    private ExpressionTree parsePrecedence(int precedence) {
        ExpressionTree lhs;

        if (this.tokenSource.peek() instanceof Operator operator
            && getPrecedenceUnary(operator.type()) > precedence) {
            // Try to parse a unary operator
            this.tokenSource.consume();
            ExpressionTree operand = parsePrecedence(getPrecedenceUnary(operator.type()));
            if (operator.type() == OperatorType.MINUS) {
                lhs = new UnaryOperationTree(operand, new Operator(OperatorType.UNARY_MINUS, operator.span()));
            } else {
                lhs = new UnaryOperationTree(operand, operator);
            }
        } else {
            // Otherwise, parse a factor
            lhs = parseFactor();
        }

        // Try to parse binary operators or ternary operator
        while (true) {
            Token nextToken = this.tokenSource.peek();
            if (nextToken instanceof Operator(var type, _)) {
                int nextPrecedence = getPrecedenceBinary(type);
                if (nextPrecedence > precedence) {
                    this.tokenSource.consume();
                    ExpressionTree rhs = parsePrecedence(nextPrecedence);
                    lhs = new BinaryOperationTree(lhs, rhs, type);
                } else if (nextToken.isOperator(OperatorType.TERNARY_CONDITION)) {
                    if (precedence < getPrecedenceTernary(OperatorType.TERNARY_CONDITION)) {
                        this.tokenSource.consume();
                        ExpressionTree trueBranch = parsePrecedence(0);
                        this.tokenSource.expectOperator(OperatorType.TERNARY_COLON);
                        ExpressionTree falseBranch = parsePrecedence(0);
                        lhs = new TernaryOperationTree(lhs, trueBranch, falseBranch);
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        return lhs;
    }

    private int getPrecedenceTernary(OperatorType type) {
        if (type == OperatorType.TERNARY_CONDITION) {
            return 2; // Ternary operator has a lower precedence than binary operators
        }
        return -1;
    }

    private int getPrecedenceBinary(OperatorType type) {
        // high number is high precedence
        return switch (type) {
            case OR -> 3;
            case AND -> 4;
            case BITWISE_OR -> 5;
            case BITWISE_XOR -> 6;
            case BITWISE_AND -> 7;
            case EQUAL, NOT_EQUAL -> 8;
            case GREATER, GREATER_EQUAL, LESS, LESS_EQUAL -> 9;
            case SHIFT_LEFT, SHIFT_RIGHT -> 10;
            case PLUS, MINUS -> 11;
            case MUL, DIV, MOD -> 12;
            default -> -1;
        };
    }

    private int getPrecedenceUnary(OperatorType type) {
        return switch (type) {
            case NOT, BITWISE_NOT, MINUS -> 13;
            default -> -1;
        };
    }

    private ExpressionTree parseFactor() {
        return switch (this.tokenSource.peek()) {
            case Separator(var type, _) when type == SeparatorType.PAREN_OPEN -> {
                this.tokenSource.consume();
                ExpressionTree expression = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                yield expression;
            }
            case Operator operator when operator.type() == OperatorType.MINUS
                || operator.type() == OperatorType.NOT
                || operator.type() == OperatorType.BITWISE_NOT -> {
                this.tokenSource.consume();
                yield new UnaryOperationTree(parseFactor(), operator);
            }
            case Keyword(var type, _) when type == Keyword.KeywordType.TRUE -> {
                Span span = this.tokenSource.consume().span();
                yield new BooleanTree(true, span);
            }
            case Keyword(var type, _) when type == Keyword.KeywordType.FALSE -> {
                Span span = this.tokenSource.consume().span();
                yield new BooleanTree(false, span);
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
