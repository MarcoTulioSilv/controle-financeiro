# Migrações do banco (Flyway)

Todo o schema do banco nasce aqui: o Hibernate roda com `ddl-auto=validate` e nunca cria nem
altera tabelas.

Nome do arquivo: `V<AAAAMMDDHHMM>__descricao.sql`
(exemplo: `V202611131600__cria_destinatario.sql`).

A versão é a data e a hora em que a migração foi escrita. Isso evita colisão quando duas
pessoas (ou dois agentes) criam migrações no mesmo dia.

Uma migração já integrada à `main` **nunca** é alterada: o Flyway guarda a soma de verificação
de cada arquivo aplicado em `flyway_schema_history` e recusa um arquivo modificado. Correções
entram como uma migração nova.
