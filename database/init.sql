
INSERT INTO hse_schema.question (nom, pictogramme, categorie, required) VALUES 
    ('alcool','alcohol', 'think', true),
    ('competence', 'competency', 'think', true),
    ('formation', 'formation', 'think', true),
    ('materiel', 'materiel', 'think', true),
    ('nombre de travailleurs', 'people', 'think', true),


    ('swp',  'swp', 'organise', true),
    ('ast', 'ast', 'organise', true),
    ('permis de travail', 'workpermit', 'organise', true),
    ('attention eboulement', 'falling-rock', 'organise', false),
    ('pelle', 'pelle', 'organise', false),
    ('attention feu', 'fire_warning', 'organise', false),
    ('test1', 'organise22', 'organise', false),
    ('test5', 'organise23', 'organise', false),
    ('test2', 'organise24', 'organise', false),
    ('test3', 'organise211', 'organise', false),
    ('test4', 'organiselast', 'organise', false),



    ('biohazard', 'biohazard', 'risk', false),
    ('electricite', 'electricity', 'risk', false),
    ('fatal', 'fatal', 'risk', false),
    ('attention risque feu ', 'fire_warning', 'risk', false),
    ('attention terrain glissant', 'slippery', 'risk', false),
    ('unknown', 'unknown', 'risk', false),
    ('attention a la marche', 'watch-steps', 'risk', false),
    ('autre', 'other-hazard', 'risk', false),


    ('antibruit', 'antibruit', 'epi', false),
    ('verre de protection du visage', 'face-protection', 'epi', false),
    ('gant', 'gant', 'epi', false),
    ('gilet', 'gilet', 'epi', false),
    ('gilet2', 'gilet2', 'epi', false),
    ('lunettes', 'glass2', 'epi', false),
    ('casque', 'helmet', 'epi', false),
    ('cache-bouche', 'mask2', 'epi', false),
    ('chaussures de protection', 'shoes', 'epi', false),
    ('uniforme', 'uniform', 'epi', false),


    ('Est-ce que je suis en bonne condition pour faire ce travail?',null , 'safety', true),
    ('Est-ce que je suis en securite pour realiser la tache?',null , 'safety', true),
    ('Executer la tache en toute securite',null , 'safety', true)