import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {
    private static final Scanner SC = new Scanner(System.in);

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n=== Avaliação Psicossocial - NR1 ===");
            System.out.println("1) Aplicar teste");
            System.out.println("2) Listar resultados");
            System.out.println("3) Gerar relatório por setor");
            System.out.println("4) Exportar CSV (já automático)");
            System.out.println("5) Sair");
            System.out.print("Escolha: ");
            String op = SC.nextLine().trim();
            switch (op) {
                case "1": aplicarTeste(); break;
                case "2": listarResultados(); break;
                case "3": gerarRelatorio(); break;
                case "4": System.out.println("Os resultados são salvos automaticamente em resultados_nr1.csv."); break;
                case "5": System.out.println("Encerrando."); return;
                default: System.out.println("Opção inválida.");
            }
        }
    }

    private static void aplicarTeste() {
        System.out.println("\n--- Aplicação de Teste ---");
        System.out.print("Nome do colaborador: ");
        String colaborador = SC.nextLine().trim();
        System.out.print("Setor: ");
        String setor = SC.nextLine().trim();

        Questionnaire q = new Questionnaire();
        Map<Factor, List<Integer>> respostas = new EnumMap<>(Factor.class);
        for (Factor f : Factor.values()) respostas.put(f, new ArrayList<>());

        System.out.println("\nPara cada afirmativa, responda de 1 a 5:");
        System.out.println("1-Nunca, 2-Raramente, 3-Às vezes, 4-Frequentemente, 5-Sempre");
        for (Question qs : q.getQuestions()) {
            int val = askLikert(qs.text);
            respostas.get(qs.factor).add(val);
        }

        TestSession session = new TestSession(respostas);
        Result result = session.computeResult(colaborador, setor);
        CSVStorage.append(result);
        System.out.println("\nResultado:");
        System.out.printf("Risco: %s | Pontuação: %.2f%%\n", result.risk, result.totalPercent);
        System.out.println("Detalhe por fator (média 1-5):");
        for (Factor f : Factor.values()) {
            System.out.printf("- %s: %.2f\n", f.label, result.factorMeans.get(f));
        }
        System.out.println("Salvo em resultados_nr1.csv");
    }

    private static int askLikert(String text) {
        while (true) {
            System.out.print(text + " (1-5): ");
            String s = SC.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v >= 1 && v <= 5) return v;
            } catch (Exception ignored) {}
            System.out.println("Entrada inválida. Informe um número de 1 a 5.");
        }
    }

    private static void listarResultados() {
        List<Result> results = CSVStorage.readAll();
        if (results.isEmpty()) {
            System.out.println("Nenhum resultado encontrado.");
            return;
        }
        System.out.println("\n--- Resultados ---");
        for (Result r : results) {
            System.out.printf("%s | Setor: %s | Colab: %s | Risco: %s | %.2f%%%n",
                    r.dateTime, r.setor, r.colaborador, r.risk, r.totalPercent);
        }
    }

    private static void gerarRelatorio() {
        List<Result> results = CSVStorage.readAll();
        if (results.isEmpty()) {
            System.out.println("Nenhum resultado para gerar relatório.");
            return;
        }
        System.out.println("\n--- Relatório por Setor ---");
        Map<String, SectorStats> stats = new HashMap<>();
        for (Result r : results) {
            stats.computeIfAbsent(r.setor, k -> new SectorStats()).add(r);
        }
        for (Map.Entry<String, SectorStats> e : stats.entrySet()) {
            String setor = e.getKey();
            SectorStats st = e.getValue();
            System.out.printf("Setor: %s | Total: %d | Baixo: %d | Médio: %d | Alto: %d | Média %%: %.2f%%%n",
                    setor, st.total, st.low, st.medium, st.high, st.avgPercent());
        }
        double geral = results.stream().mapToDouble(r -> r.totalPercent).average().orElse(0);
        System.out.printf("Geral média de risco: %.2f%%%n", geral);
    }

    enum Factor {
        DEMANDA("Demanda"), CONTROLE("Controle"), SUPORTE("Suporte"),
        RECOMPENSA("Recompensa"), AMBIENTE("Ambiente"), VIOLENCIA("Violência");
        final String label;
        Factor(String l) { this.label = l; }
    }

    static class Question {
        final String text; final Factor factor;
        Question(String t, Factor f) { this.text = t; this.factor = f; }
    }

    static class Questionnaire {
        private final List<Question> questions = new ArrayList<>();
        Questionnaire() {
            // Demanda
            questions.add(new Question("Tenho volume de trabalho elevado", Factor.DEMANDA));
            questions.add(new Question("Tenho prazos apertados com pressão constante", Factor.DEMANDA));
            questions.add(new Question("Tenho exigências emocionais intensas no trabalho", Factor.DEMANDA));
            // Controle
            questions.add(new Question("Tenho pouca autonomia para decidir como executar tarefas", Factor.CONTROLE));
            questions.add(new Question("Tenho pouca influência nas decisões que afetam meu trabalho", Factor.CONTROLE));
            questions.add(new Question("Tenho tarefas muito rígidas sem possibilidade de ajuste", Factor.CONTROLE));
            // Suporte
            questions.add(new Question("Tenho pouco apoio da liderança quando preciso", Factor.SUPORTE));
            questions.add(new Question("Tenho pouco apoio dos colegas para cumprir demandas", Factor.SUPORTE));
            questions.add(new Question("Tenho dificuldade de acesso a recursos/treinamentos", Factor.SUPORTE));
            // Recompensa
            questions.add(new Question("Tenho reconhecimento insuficiente pelo desempenho", Factor.RECOMPENSA));
            questions.add(new Question("Tenho insegurança quanto à estabilidade/valorização", Factor.RECOMPENSA));
            questions.add(new Question("Tenho progressão de carreira limitada/obscura", Factor.RECOMPENSA));
            // Ambiente
            questions.add(new Question("Tenho condições físicas inadequadas (ruído, temperatura, ergonomia)", Factor.AMBIENTE));
            questions.add(new Question("Tenho ferramentas/equipamentos inadequados", Factor.AMBIENTE));
            questions.add(new Question("Tenho interrupções frequentes que atrapalham o foco", Factor.AMBIENTE));
            // Violência
            questions.add(new Question("Tenho exposição a assédio moral/verbal", Factor.VIOLENCIA));
            questions.add(new Question("Tenho conflitos frequentes com clientes/usuários", Factor.VIOLENCIA));
            questions.add(new Question("Tenho medo de sofrer agressões no trabalho", Factor.VIOLENCIA));
        }
        List<Question> getQuestions() { return questions; }
    }

    static class TestSession {
        private final Map<Factor, List<Integer>> respostas;
        TestSession(Map<Factor, List<Integer>> respostas) { this.respostas = respostas; }
        Result computeResult(String colaborador, String setor) {
            Map<Factor, Double> means = new EnumMap<>(Factor.class);
            int totalItems = 0; int totalScore = 0;
            for (Factor f : Factor.values()) {
                List<Integer> vals = respostas.getOrDefault(f, Collections.emptyList());
                totalItems += vals.size();
                int sum = vals.stream().mapToInt(i -> i).sum();
                totalScore += sum;
                double mean = vals.isEmpty() ? 0 : (sum * 1.0) / vals.size();
                means.put(f, mean);
            }
            int maxScore = totalItems * 5;
            double percent = maxScore == 0 ? 0 : (totalScore * 100.0) / maxScore;
            String risk = classify(percent);
            return new Result(colaborador, setor, now(), percent, risk, means);
        }
        private String classify(double percent) {
            if (percent < 33) return "Baixo";
            if (percent < 66) return "Médio";
            return "Alto";
        }
        private String now() {
            return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    static class Result {
        final String colaborador, setor, dateTime, risk; final double totalPercent;
        final Map<Factor, Double> factorMeans;
        Result(String c, String s, String d, double p, String r, Map<Factor, Double> m) {
            colaborador = c; setor = s; dateTime = d; totalPercent = p; risk = r; factorMeans = m;
        }
        String toCSV() {
            StringBuilder sb = new StringBuilder();
            sb.append(escape(dateTime)).append(',').append(escape(setor)).append(',').append(escape(colaborador))
              .append(',').append(String.format(Locale.US, "%.2f", totalPercent)).append(',').append(risk);
            for (Factor f : Factor.values()) sb.append(',').append(String.format(Locale.US, "%.2f", factorMeans.getOrDefault(f, 0.0)));
            return sb.toString();
        }
        static Result fromCSV(String line) {
            String[] parts = splitCSV(line);
            if (parts.length < 5 + Factor.values().length) return null;
            String d = parts[0], s = parts[1], c = parts[2];
            double p = Double.parseDouble(parts[3]);
            String r = parts[4];
            Map<Factor, Double> m = new EnumMap<>(Factor.class);
            for (int i = 0; i < Factor.values().length; i++) {
                m.put(Factor.values()[i], Double.parseDouble(parts[5 + i]));
            }
            return new Result(c, s, d, p, r, m);
        }
        static String header() {
            StringBuilder sb = new StringBuilder("Data,Setor,Colaborador,PontuacaoPercent,Risco");
            for (Factor f : Factor.values()) sb.append(',').append("Media_" + f.name());
            return sb.toString();
        }
        private static String escape(String s) {
            if (s.contains(",") || s.contains("\"")) return '"' + s.replace("\"", "\"\"") + '"';
            return s;
        }
        private static String[] splitCSV(String line) {
            List<String> out = new ArrayList<>();
            boolean inQ = false; StringBuilder cur = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (inQ) {
                    if (ch == '"') {
                        if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                        else inQ = false;
                    } else cur.append(ch);
                } else {
                    if (ch == ',') { out.add(cur.toString()); cur.setLength(0); }
                    else if (ch == '"') inQ = true;
                    else cur.append(ch);
                }
            }
            out.add(cur.toString());
            return out.toArray(new String[0]);
        }
    }

    static class CSVStorage {
        private static final Path FILE = Paths.get("resultados_nr1.csv");
        static void append(Result r) {
            try {
                boolean exists = Files.exists(FILE);
                try (BufferedWriter w = Files.newBufferedWriter(FILE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                    if (!exists) w.write(Result.header() + System.lineSeparator());
                    w.write(r.toCSV()); w.write(System.lineSeparator());
                }
            } catch (IOException e) {
                System.out.println("Falha ao salvar CSV: " + e.getMessage());
            }
        }
        static List<Result> readAll() {
            List<Result> list = new ArrayList<>();
            if (!Files.exists(FILE)) return list;
            try (BufferedReader br = Files.newBufferedReader(FILE)) {
                String line; boolean first = true;
                while ((line = br.readLine()) != null) {
                    if (first) { first = false; continue; }
                    Result r = Result.fromCSV(line);
                    if (r != null) list.add(r);
                }
            } catch (IOException e) {
                System.out.println("Falha ao ler CSV: " + e.getMessage());
            }
            return list;
        }
    }

    static class SectorStats {
        int total = 0, low = 0, medium = 0, high = 0; double sumPercent = 0;
        void add(Result r) {
            total++; sumPercent += r.totalPercent;
            switch (r.risk) {
                case "Baixo": low++; break;
                case "Médio": medium++; break;
                default: high++; break;
            }
        }
        double avgPercent() { return total == 0 ? 0 : sumPercent / total; }
    }
}