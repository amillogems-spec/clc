package com.webseicho.calculator;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MainActivity extends Activity {

    private TextView exprView, resultView;
    private final StringBuilder expr = new StringBuilder();

    private static final String[][] KEYS = {
            {"C", "⌫", "%", "÷"},
            {"7", "8", "9", "×"},
            {"4", "5", "6", "−"},
            {"1", "2", "3", "+"},
            {"00", "0", ".", "="}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#121212"));
        root.setPadding(dp(12), dp(24), dp(12), dp(12));

        // Display area
        LinearLayout display = new LinearLayout(this);
        display.setOrientation(LinearLayout.VERTICAL);
        display.setGravity(Gravity.BOTTOM | Gravity.END);
        display.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.addView(display, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.2f));

        exprView = new TextView(this);
        exprView.setTextColor(Color.parseColor("#B0B0B0"));
        exprView.setTextSize(30);
        exprView.setGravity(Gravity.END);
        exprView.setMaxLines(2);
        display.addView(exprView);

        resultView = new TextView(this);
        resultView.setTextColor(Color.WHITE);
        resultView.setTextSize(44);
        resultView.setTypeface(Typeface.DEFAULT_BOLD);
        resultView.setGravity(Gravity.END);
        resultView.setMaxLines(1);
        display.addView(resultView);

        // Keypad
        LinearLayout pad = new LinearLayout(this);
        pad.setOrientation(LinearLayout.VERTICAL);
        root.addView(pad, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 2.6f));

        for (String[] row : KEYS) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            pad.addView(r, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
            for (String key : row) {
                r.addView(makeButton(key));
            }
        }

        setContentView(root);
    }

    private Button makeButton(final String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(24);
        b.setTextColor(Color.WHITE);

        String bg;
        if (label.equals("=")) bg = "#FF9800";
        else if ("÷×−+".contains(label)) bg = "#3949AB";
        else if (label.equals("C") || label.equals("⌫") || label.equals("%")) bg = "#455A64";
        else bg = "#2A2A2A";

        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.parseColor(bg));
        shape.setCornerRadius(dp(18));
        b.setBackground(shape);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        b.setLayoutParams(lp);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onKey(label);
            }
        });
        return b;
    }

    private void onKey(String key) {
        switch (key) {
            case "C":
                expr.setLength(0);
                resultView.setText("");
                break;
            case "⌫":
                if (expr.length() > 0) expr.deleteCharAt(expr.length() - 1);
                break;
            case "=":
                try {
                    String res = format(evaluate(expr.toString()));
                    expr.setLength(0);
                    expr.append(res);
                    exprView.setText(expr.toString());
                    resultView.setText("");
                } catch (Exception e) {
                    resultView.setText("Error");
                }
                return;
            default:
                expr.append(key);
        }
        exprView.setText(expr.toString());
        if (expr.length() > 0) {
            try {
                resultView.setText(format(evaluate(expr.toString())));
            } catch (Exception e) {
                resultView.setText("");
            }
        } else {
            resultView.setText("");
        }
    }

    // ---------- Expression evaluator ----------

    private double evaluate(String input) {
        final String s = input.replace("÷", "/").replace("×", "*").replace("−", "-");
        return new Object() {
            int pos = 0;

            double parse() {
                double v = parseExpr();
                if (pos < s.length()) throw new RuntimeException("Unexpected");
                return v;
            }

            double parseExpr() {
                double v = parseTerm();
                while (pos < s.length()) {
                    char c = s.charAt(pos);
                    if (c == '+') { pos++; v += parseTerm(); }
                    else if (c == '-') { pos++; v -= parseTerm(); }
                    else break;
                }
                return v;
            }

            double parseTerm() {
                double v = parseFactor();
                while (pos < s.length()) {
                    char c = s.charAt(pos);
                    if (c == '*') { pos++; v *= parseFactor(); }
                    else if (c == '/') {
                        pos++;
                        double d = parseFactor();
                        if (d == 0) throw new ArithmeticException("Div by zero");
                        v /= d;
                    } else break;
                }
                return v;
            }

            double parseFactor() {
                double v;
                if (pos < s.length() && s.charAt(pos) == '-') {
                    pos++;
                    v = -parseFactor();
                } else {
                    int start = pos;
                    while (pos < s.length()
                            && (Character.isDigit(s.charAt(pos)) || s.charAt(pos) == '.')) pos++;
                    if (start == pos) throw new RuntimeException("Number expected");
                    v = Double.parseDouble(s.substring(start, pos));
                }
                while (pos < s.length() && s.charAt(pos) == '%') {
                    pos++;
                    v /= 100;
                }
                return v;
            }
        }.parse();
    }

    private String format(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) throw new ArithmeticException();
        BigDecimal bd = new BigDecimal(d).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros();
        if (bd.scale() < 0) bd = bd.setScale(0);
        return bd.toPlainString();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
