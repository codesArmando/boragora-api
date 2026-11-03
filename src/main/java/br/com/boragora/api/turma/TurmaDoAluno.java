package br.com.boragora.api.turma;

/** Turma dentro das respostas de aluno, com o grupo para a tela montar "Manhã · Van 2". */
public record TurmaDoAluno(Long id, String nome, GrupoDaTurma grupo) {

    public static TurmaDoAluno de(Turma turma) {
        return new TurmaDoAluno(turma.getId(), turma.getNome(), GrupoDaTurma.de(turma.getGrupo()));
    }
}
