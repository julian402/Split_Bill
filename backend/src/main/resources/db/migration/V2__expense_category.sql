-- Categoria de cada gasto (rediseno): comida, mercado, transporte, etc. Los gastos que ya existian
-- quedan como OTHER. PAYMENT es un pago entre integrantes registrado desde la liquidacion.
ALTER TABLE expenses
    ADD COLUMN exp_category VARCHAR(20) NOT NULL DEFAULT 'OTHER'
        CHECK (exp_category IN ('FOOD', 'GROCERIES', 'TRANSPORT', 'LODGING', 'ENTERTAINMENT',
                                'SERVICES', 'OTHER', 'PAYMENT'));
