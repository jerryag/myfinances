import { useState, useEffect, useRef } from 'react';
import { api } from '../services/api';
import { usePageTitle } from '../context/PageTitleContext';
import { FaPlus, FaMinus, FaTrash, FaChevronLeft, FaChevronRight, FaRegComment, FaComment, FaHistory, FaSync, FaArrowUp, FaArrowDown } from 'react-icons/fa';
import { MessageModal } from '../components/MessageModal';
import { ConfirmationModal } from '../components/ConfirmationModal';
import { getIcon } from '../utils/IconRepository';
import { IconSelector } from '../components/IconSelector';

export function MonthlyPlanning() {
    const { setTitle } = usePageTitle();
    const [currentDate, setCurrentDate] = useState(new Date());
    const [monthData, setMonthData] = useState(null);
    const [transactionTypes, setTransactionTypes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [messageModal, setMessageModal] = useState({ isOpen: false, title: '', message: '', type: 'info' });
    const [editingRemark, setEditingRemark] = useState({ isOpen: false, transactionId: null, currentRemark: '' });
    const [editingIcon, setEditingIcon] = useState({ isOpen: false, transactionId: null, currentIcon: '' });

    // Filters State
    const [filters, setFilters] = useState({ day: '', types: [], typeText: '' });

    // Confirmation Modal state
    const [confirmModal, setConfirmModal] = useState({ isOpen: false, transactionId: null });

    // Track focused row for UI highlighting
    const [focusedRowId, setFocusedRowId] = useState(null);
    const focusedRowIdRef = useRef(null);
    const pendingSortedData = useRef(null);
    // Ref for table scrolling
    const tableContainerRef = useRef(null);

    // Derived state for filtering
    const filteredTransactions = (() => {
        // We can't use useMemo easily if it's not imported, and I can't update line 1 in this same steps easily without context.
        // Actually, just calculating it in render body (before return) is fine for "derived state" in functional components,
        // IF we don't need it in useEffect dependencies (we don't) and cost is low.
        // BUT I need it in handlers? Handlers clearly can access state variables.
        // BUT variables defined in render are not accessible in handlers defined outside render scope?
        // Wait, functional components: handlers are defined inside the function body.
        // So if I define `const filteredTransactions = ...` at the top of `MonthlyPlanning` function body,
        // it is accessible to all handlers defined below it!
        // Yes! No need for `useMemo` if performance isn't critical (filtering ~100 items is fast).
        // I'll just hoist the calculation to the top of the function body.
        if (!monthData || !monthData.transactions) return [];
        return monthData.transactions.filter(t => {
            // Day Filter
            if (filters.day && t.day && parseInt(t.day) !== parseInt(filters.day)) return false;

            // Type Text Filter (New)
            if (filters.typeText) {
                const typeDef = transactionTypes.find(type => type.id === t.transactionTypeId);
                if (!typeDef || !typeDef.description.toLowerCase().includes(filters.typeText.toLowerCase())) {
                    return false;
                }
            }

            // Type Filter
            if (filters.types.length > 0) {
                const typeDef = transactionTypes.find(type => type.id === t.transactionTypeId);
                if (typeDef) {
                    if (!filters.types.includes(typeDef.type)) return false;
                } else {
                    if (t.transactionTypeId) return false;
                    return false;
                }
            }
            return true;
        });
    })();

    const handleRemarkSave = async (remark) => {
        if (!editingRemark.transactionId) return;

        // Optimistic update
        if (monthData && monthData.transactions) {
            const updatedTransactions = monthData.transactions.map(t =>
                t.id === editingRemark.transactionId ? { ...t, remark: remark } : t
            );
            const targetTransaction = updatedTransactions.find(t => t.id === editingRemark.transactionId);

            setMonthData(prev => ({ ...prev, transactions: updatedTransactions }));

            // Save to backend
            if (targetTransaction) {
                handleTransactionBlur(targetTransaction);
            }
        }
        setEditingRemark({ isOpen: false, transactionId: null, currentRemark: '' });
    };

    const handleIconSave = (newIconName) => {
        if (!editingIcon.transactionId) return;

        // Optimistic update
        if (monthData && monthData.transactions) {
            const updatedTransactions = monthData.transactions.map(t =>
                t.id === editingIcon.transactionId ? { ...t, iconName: newIconName } : t
            );
            const targetTransaction = updatedTransactions.find(t => t.id === editingIcon.transactionId);

            setMonthData(prev => ({ ...prev, transactions: updatedTransactions }));

            // Save to backend
            if (targetTransaction) {
                const updatedT = { ...targetTransaction, iconName: newIconName };
                handleTransactionBlur(updatedT);
            }
        }
        setEditingIcon({ isOpen: false, transactionId: null, currentIcon: '' });
    };

    useEffect(() => {
        focusedRowIdRef.current = focusedRowId;

        // If we switch focus to a different row AND we have pending sorted data, apply it now
        if (pendingSortedData.current && focusedRowId !== pendingSortedData.current.triggerRowId) {
            const dataToApply = pendingSortedData.current.data;
            setMonthData(prev => {
                if (!prev || !prev.transactions) return prev;
                return mergeWithTemps(dataToApply, prev.transactions);
            });
            pendingSortedData.current = null;
        }
    }, [focusedRowId]);

    useEffect(() => {
        setTitle('Planejamento Mensal');
        fetchTransactionTypes();
    }, []);

    useEffect(() => {
        fetchMonthData(currentDate.getMonth() + 1, currentDate.getFullYear());
    }, [currentDate]);

    const fetchTransactionTypes = async () => {
        try {
            const response = await api.get('/transaction-types', { params: { size: 100 } });
            setTransactionTypes(response.data.content);
        } catch (error) {
            console.error('Erro ao buscar tipos:', error);
        }
    };

    const fetchMonthData = async (month, year) => {
        setLoading(true);
        try {
            const response = await api.get(`/transaction-months/${year}/${month}`);
            setMonthData(response.data);
        } catch (error) {
            console.error('Erro ao buscar dados do mês:', error);
            setMessageModal({ isOpen: true, title: 'Erro', message: 'Erro ao carregar dados do mês.', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    const handleInitialBalanceSave = async (newValue) => {
        // Optimistic update
        setMonthData(prev => ({ ...prev, initialBalance: newValue }));

        try {
            await api.patch(`/transaction-months/${monthData.id}/initial-balance`, newValue, {
                headers: { 'Content-Type': 'application/json' }
            });
        } catch (error) {
            console.error('Erro ao atualizar saldo inicial:', error);
            setMessageModal({ isOpen: true, title: 'Erro', message: 'Erro ao atualizar saldo inicial.', type: 'error' });
        }
    };

    const isTempId = (id) => String(id).startsWith('temp-');

    const mergeWithTemps = (backendData, currentTransactions) => {
        const temps = currentTransactions.filter(t => isTempId(t.id));
        return {
            ...backendData,
            transactions: [...backendData.transactions, ...temps]
        };
    };

    const handleAddLine = async () => {
        const newTransaction = {
            id: `temp-${Date.now()}`,
            day: '', // Blank
            description: '',
            transactionTypeId: '', // Blank
            amount: 0,
            status: 'PENDING',
            remark: ''
        };

        setMonthData(prev => ({
            ...prev,
            transactions: [...prev.transactions, newTransaction]
        }));

        // Scroll to bottom to show new line
        setTimeout(() => {
            if (tableContainerRef.current) {
                tableContainerRef.current.scrollTop = tableContainerRef.current.scrollHeight;
            }
            // Focus on the first field (Type Select)
            const element = document.getElementById(`type-select-${newTransaction.id}`);
            if (element) {
                element.focus();
            }
        }, 100);
    };

    const handleTransactionChange = (id, field, value) => {
        setMonthData(prev => ({
            ...prev,
            transactions: prev.transactions.map(t => t.id === id ? { ...t, [field]: value } : t)
        }));
    };

    const handleFilterChange = (e) => {
        const { name, value, checked, type } = e.target;
        if (type === 'checkbox') {
            setFilters(prev => {
                const newTypes = checked
                    ? [...prev.types, value]
                    : prev.types.filter(t => t !== value);
                return { ...prev, types: newTypes };
            });
        } else {
            setFilters(prev => ({ ...prev, [name]: value }));
        }
    };

    const handleClearFilters = () => {
        setFilters({ day: '', types: [], typeText: '' });
    };

    const handleScrollTop = () => {
        if (!tableContainerRef.current || filteredTransactions.length === 0) return;
        tableContainerRef.current.scrollTop = 0;

        const first = filteredTransactions[0];
        setFocusedRowId(first.id);

        // Focus logic
        setTimeout(() => {
            const el = document.getElementById(`type-select-${first.id}`);
            if (el) el.focus();
        }, 50);
    };

    const handleScrollBottom = () => {
        if (!tableContainerRef.current || filteredTransactions.length === 0) return;
        tableContainerRef.current.scrollTop = tableContainerRef.current.scrollHeight;

        const last = filteredTransactions[filteredTransactions.length - 1];
        setFocusedRowId(last.id);

        // Focus logic
        setTimeout(() => {
            const el = document.getElementById(`type-select-${last.id}`);
            if (el) el.focus();
        }, 50);
    };

    const handleTransactionBlur = async (transaction) => {
        // Validation for Save/Create
        if (isTempId(transaction.id)) {
            // Only create if we have minimum required fields
            if (!transaction.day || !transaction.transactionTypeId) {
                return; // Keep as draft
            }

            // Prepare payload (remove temp ID)
            const payload = { ...transaction, id: null };

            try {
                const response = await api.post(`/transaction-months/${monthData.id}/transactions`, payload);

                // Identify the new real transaction (ID present in response but not in current real IDs)
                // Actually, simpler: The response returns the full state. The new transaction is the one that correlates to our payload?
                // No, we can diff the IDs.

                // We need to update state IN PLACE to avoid jumping.
                setMonthData(prev => {
                    // Find the new real transaction from response
                    // It's a bit hard to match exactly if we don't have the ID.
                    // But we know 'prev' has some real IDs. 'response' has those + 1 new one.
                    // (Assuming no concurrent modifications by others, which is fine for now).

                    const prevRealIds = new Set(prev.transactions.filter(t => !isTempId(t.id)).map(t => t.id));
                    const newTransactionFromServer = response.data.transactions.find(t => !prevRealIds.has(t.id));

                    if (!newTransactionFromServer) {
                        // Fallback: just use response data if we can't find it (shouldn't happen)
                        const remainingTemps = prev.transactions.filter(t => isTempId(t.id) && t.id !== transaction.id);
                        return {
                            ...response.data,
                            transactions: [...response.data.transactions, ...remainingTemps]
                        };
                    }

                    // Update focus tracker to new ID so we don't lose track
                    focusedRowIdRef.current = newTransactionFromServer.id;
                    setFocusedRowId(newTransactionFromServer.id);

                    // Defer Sort
                    pendingSortedData.current = {
                        data: response.data,
                        triggerRowId: newTransactionFromServer.id
                    };

                    // Replace temp with real in place
                    return {
                        ...prev, // Keep current root state (roughly)
                        // Actually we should update root fields like balance from response, but keep transactions order
                        initialBalance: response.data.initialBalance,
                        id: response.data.id,
                        month: response.data.month,
                        year: response.data.year,

                        transactions: prev.transactions.map(t => {
                            if (t.id === transaction.id) return newTransactionFromServer;
                            return t;
                        })
                    };
                });
            } catch (error) {
                console.error('Erro ao criar transação:', error);
                setMessageModal({ isOpen: true, title: 'Erro', message: 'Erro ao criar linha.', type: 'error' });
            }
        } else {
            // Update existing
            try {
                const response = await api.put(`/transaction-months/transactions/${transaction.id}`, transaction);

                // Check if user is still focused on this row via Ref (current state)
                if (focusedRowIdRef.current === transaction.id) {
                    // Update in-place to avoid jump
                    setMonthData(prev => {
                        if (!prev || !prev.transactions) return prev;
                        const updatedBackendT = response.data.transactions.find(t => t.id === transaction.id);
                        if (!updatedBackendT) return prev;

                        const newTransactions = prev.transactions.map(t =>
                            t.id === transaction.id ? updatedBackendT : t
                        );

                        return {
                            ...response.data, // Keep potential root updates (balance, etc)
                            transactions: newTransactions // But override transactions with our preserved order
                        };
                    });

                    // Store for later sort
                    pendingSortedData.current = {
                        data: response.data,
                        triggerRowId: transaction.id
                    };
                } else {
                    // User moved away, safe to sort
                    setMonthData(prev => {
                        if (!prev || !prev.transactions) return prev;
                        return mergeWithTemps(response.data, prev.transactions);
                    });
                }
            } catch (error) {
                console.error('Erro ao atualizar transação:', error);
            }
        }
    };

    const handleDeleteTransaction = (id) => {
        if (isTempId(id)) {
            setMonthData(prev => ({
                ...prev,
                transactions: prev.transactions.filter(t => t.id !== id)
            }));
            return;
        }
        // Open confirmation modal for saved transactions
        setConfirmModal({ isOpen: true, transactionId: id });
    };

    const confirmDeleteTransaction = async () => {
        const id = confirmModal.transactionId;
        if (!id) return;

        try {
            const response = await api.delete(`/transaction-months/transactions/${id}`);
            setMonthData(prev => mergeWithTemps(response.data, prev.transactions));
        } catch (error) {
            console.error('Erro ao excluir:', error);
            setMessageModal({ isOpen: true, title: 'Erro', message: 'Erro ao excluir linha.', type: 'error' });
        } finally {
            setConfirmModal({ isOpen: false, transactionId: null });
        }
    };

    const handleImportLastValue = async (transaction) => {
        console.log('Import clicking for:', transaction);
        if (!transaction.transactionTypeId) {
            setMessageModal({ isOpen: true, title: 'Aviso', message: 'Selecione uma categoria primeiro.', type: 'warning' });
            return;
        }

        try {
            console.log('Fetching last value...');
            const response = await api.get('/transaction-months/last-value', {
                params: {
                    transactionTypeId: transaction.transactionTypeId,
                    description: transaction.description
                }
            });
            console.log('Response:', response);

            const lastValue = response.data;
            console.log('Last Value:', lastValue);

            if (lastValue !== null) {
                handleTransactionChange(transaction.id, 'amount', lastValue);
                // Auto-save
                const updatedT = { ...transaction, amount: lastValue };
                handleTransactionBlur(updatedT);
            } else {
                setMessageModal({ isOpen: true, title: 'Info', message: 'Nenhum valor anterior encontrado.', type: 'info' });
            }

        } catch (error) {
            console.error('Erro ao buscar último valor:', error);
            setMessageModal({ isOpen: true, title: 'Erro', message: 'Erro ao buscar último valor.', type: 'error' });
        }
    };

    const changeMonth = (offset) => {
        const newDate = new Date(currentDate.setMonth(currentDate.getMonth() + offset));
        setCurrentDate(new Date(newDate));
    };

    const formatCurrency = (value) => {
        return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
    };

    const calculateTotals = () => {
        if (!monthData) return { plannedIncome: 0, plannedExpense: 0, realizedIncome: 0, realizedExpense: 0 };
        // Totals should probably reflect ALL transactions for correct balance calculation? 
        // OR filtered totals? Usually Balance (Saldo Inicial + Totals) refers to the real account state, so it shouldn't be filtered.
        // But the table view is filtered.
        // User asked for "filters". Usually filters affect the view list. 
        // IMPORTANT: The "Finished" (Realizado) footer and Header Balance often refer to the actual month state.
        // If I filter by "Day 5", showing a balance of only day 5 transactions might be confusing if the "Saldo Inicial" is for the whole month.
        // I will currently filter ONLY the table list, but KEEP the totals calculation on the FULL list, unless user specified otherwise.
        // However, if the user wants to see "How much did I spend on food?", he might expect the totals to update.
        // Given complexity, I'll filter the VIEW list but keep Totals on ALL data for now (or maybe filtered? "Saldo" column running balance heavily depends on previous transactions...).
        // RUNNING BALANCE logic is tricky with filters. If I filter out previous days, the running balance will be wrong if it starts from 0.
        // It starts from `monthData.initialBalance`.
        // If I hide Day 1, and show Day 5, should Day 5 start with Initial Balance or Initial + Day 1?
        // Standard behavior: Filtering hides rows. Running balance usually reflects the visible rows accumulated from the *start point*?
        // Actually, for a checkbook style app, running balance is usually tied to the order. Hiding rows breaks the continuity of the running balance column.
        // I will just filter the `filteredTransactions` for the map, but calculating running balance...
        // If I skip rows, the running balance of the first visible row should probably include the invisible rows previous to it?
        // OR, simply recalculate running balance based on visible rows starting from Initial Balance.
        // Let's implement strict visual filtering. The Running Balance column might look weird (jumps), but that's expected. 
        // Wait, if I filter "Income", the "Saldo" column (Balance) becomes meaningless if it doesn't account for Expenses.
        // I will apply the filter to the `map` loop.

        // Let's first define derived state for rendering.
        return calculateActualTotals(monthData.transactions);
    };

    const calculateActualTotals = (transactions) => {
        if (!transactions) return { plannedIncome: 0, plannedExpense: 0, realizedIncome: 0, realizedExpense: 0 };
        let plannedIncome = 0, plannedExpense = 0, realizedIncome = 0, realizedExpense = 0;

        transactions.forEach(t => {
            const type = transactionTypes.find(type => type.id === t.transactionTypeId);
            const isIncome = type?.type === 'INCOME';

            // Planned (All)
            if (isIncome) plannedIncome += t.amount || 0;
            else plannedExpense += t.amount || 0;

            // Realized (Paid/Received)
            if (t.status === 'COMPLETED') {
                if (isIncome) realizedIncome += t.amount || 0;
                else realizedExpense += t.amount || 0;
            }
        });
        return { plannedIncome, plannedExpense, realizedIncome, realizedExpense };
    }

    const totals = calculateTotals();
    const plannedBalance = (parseFloat(monthData?.initialBalance) || 0) + totals.plannedIncome - totals.plannedExpense;
    const realizedBalance = (parseFloat(monthData?.initialBalance) || 0) + totals.realizedIncome - totals.realizedExpense;

    if (loading && !monthData) return <div style={{ padding: '20px', textAlign: 'center' }}>Carregando...</div>;



    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 120px)', maxWidth: '1200px', margin: '0 auto', padding: '10px' }}>

            {/* Header: Select Month & Initial Balance */}
            <div className="card" style={{ marginBottom: '15px', padding: '15px', display: 'flex', flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', background: '#2a2a2a', color: '#fff' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                    <button onClick={() => changeMonth(-1)} className="btn-icon" style={{ background: 'transparent', border: 'none', color: 'inherit', cursor: 'pointer', display: 'flex' }}><FaChevronLeft /></button>
                    <h2 style={{ margin: 0, textTransform: 'capitalize', width: '200px', textAlign: 'center' }}>
                        {currentDate.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })}
                    </h2>
                    <button onClick={() => changeMonth(1)} className="btn-icon" style={{ background: 'transparent', border: 'none', color: 'inherit', cursor: 'pointer', display: 'flex' }}><FaChevronRight /></button>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <label>Saldo inicial de {currentDate.toLocaleDateString('pt-BR', { month: 'long' })}:</label>
                    <div style={{ padding: '8px', borderRadius: '4px', border: '1px solid #555', width: '120px', backgroundColor: '#333' }}>
                        <MoneyInput
                            value={monthData?.initialBalance || 0}
                            onSave={handleInitialBalanceSave}
                            textColor="white"
                        />
                    </div>
                </div>
            </div>

            {/* Filters Panel */}
            <div style={{
                marginBottom: '15px',
                padding: '15px',
                border: '1px solid #ddd',
                borderRadius: '8px',
                background: '#f9f9f9',
                color: '#333'
            }}>
                <div style={{ marginBottom: '10px', fontWeight: 'bold' }}>Filtros:</div>
                <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: '30px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <label>Dia:</label>
                        <input
                            type="number"
                            name="day"
                            min="1"
                            max="31"
                            value={filters.day}
                            onChange={handleFilterChange}
                            style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ccc', width: '60px', textAlign: 'center' }}
                        />
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <label>Tipo:</label>
                        <input
                            type="text"
                            name="typeText"
                            value={filters.typeText}
                            onChange={handleFilterChange}
                            placeholder="Filtrar por nome..."
                            style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ccc', width: '150px' }}
                        />
                    </div>
                    <div style={{ display: 'flex', gap: '20px', alignItems: 'center' }}>
                        <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
                            <input
                                type="checkbox"
                                name="types"
                                value="INCOME"
                                checked={filters.types.includes('INCOME')}
                                onChange={handleFilterChange}
                                style={{ marginRight: '8px', cursor: 'pointer', transform: 'scale(1.2)' }}
                            /> Entrada
                        </label>
                        <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
                            <input
                                type="checkbox"
                                name="types"
                                value="EXPENSE"
                                checked={filters.types.includes('EXPENSE')}
                                onChange={handleFilterChange}
                                style={{ marginRight: '8px', cursor: 'pointer', transform: 'scale(1.2)' }}
                            /> Saída
                        </label>
                    </div>
                    <div style={{ marginLeft: 'auto' }}>
                        <button
                            onClick={handleClearFilters}
                            style={{
                                padding: '8px 16px',
                                border: '1px solid #ccc',
                                borderRadius: '4px',
                                background: 'white',
                                cursor: 'pointer'
                            }}
                        >
                            Limpar
                        </button>
                    </div>
                </div>
            </div>

            {/* Spreadsheet Table */}
            <div ref={tableContainerRef} style={{ flex: 1, overflowY: 'auto', border: '1px solid #333', borderRadius: '8px', background: 'var(--card-bg)', marginBottom: '100px' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', tableLayout: 'fixed' }}>
                    <thead style={{ position: 'sticky', top: 0, background: '#2a2a2a', zIndex: 1, color: '#fff', boxShadow: '0 1px 2px rgba(0,0,0,0.5)' }}>
                        <tr>
                            <th style={{ width: '200px', padding: '10px', borderBottom: '1px solid #333' }}>Tipo Lançamento</th>
                            <th style={{ width: '60px', padding: '10px', borderBottom: '1px solid #333' }}>Dia</th>
                            <th style={{ width: '40px', padding: '10px', borderBottom: '1px solid #333', textAlign: 'center' }}>Cat</th>
                            <th style={{ padding: '10px', borderBottom: '1px solid #333' }}>Descrição</th>
                            <th style={{ width: '40px', padding: '10px', borderBottom: '1px solid #333', textAlign: 'center' }}></th>
                            <th style={{ width: '120px', padding: '10px', borderBottom: '1px solid #333' }}>Valor</th>
                            <th style={{ width: '120px', padding: '10px', borderBottom: '1px solid #333' }}>Saldo</th>
                            <th style={{ width: '80px', padding: '10px', borderBottom: '1px solid #333', textAlign: 'center' }}>Realizado</th>
                            <th style={{ width: '40px', padding: '10px', borderBottom: '1px solid #333' }}>Obs</th>
                            <th style={{ width: '50px', padding: '10px', borderBottom: '1px solid #333' }}>#</th>
                        </tr>
                    </thead>
                    <tbody>
                        {(() => {
                            let runningBalance = parseFloat(monthData?.initialBalance || 0);

                            return filteredTransactions.map(t => {
                                const typeDef = transactionTypes.find(type => type.id === t.transactionTypeId);
                                const isIncome = typeDef?.type === 'INCOME';
                                const amount = t.amount || 0;

                                if (isIncome) runningBalance += amount;
                                else runningBalance -= amount;

                                const hasRemark = t.remark && t.remark.trim().length > 0;
                                const isFilterActive = !!filters.day || filters.types.length > 0 || !!filters.typeText;

                                return (
                                    <tr key={t.id} className={`transaction-row ${focusedRowId === t.id ? 'active' : ''}`} style={{ borderBottom: '1px solid #333' }}>
                                        <td style={{ padding: '5px' }}>
                                            <select
                                                id={`type-select-${t.id}`}
                                                value={t.transactionTypeId || ''}
                                                onFocus={() => setFocusedRowId(t.id)}
                                                onChange={e => {
                                                    const newTypeId = e.target.value ? parseInt(e.target.value) : '';

                                                    // Auto-fill Day Logic
                                                    let updates = { transactionTypeId: newTypeId };

                                                    if (newTypeId) {
                                                        const typeDef = transactionTypes.find(type => type.id === newTypeId);
                                                        if (typeDef) {
                                                            let targetDay = typeDef.defaultDay || 0; // 0 means not set, will defer to 1
                                                            if (targetDay === 0) targetDay = 1;

                                                            // Validate against max days in this month
                                                            // currentDate is state: month is 0-indexed in JS Date, but we need the displayed month length
                                                            // displayed month is currentDate.
                                                            const year = currentDate.getFullYear();
                                                            const month = currentDate.getMonth(); // 0-11
                                                            const daysInMonth = new Date(year, month + 1, 0).getDate();

                                                            if (targetDay > daysInMonth) {
                                                                targetDay = daysInMonth;
                                                            }

                                                            updates.day = targetDay;
                                                        }

                                                        // Auto-fill Amount Logic
                                                        if (typeDef.defaultAmount && parseFloat(typeDef.defaultAmount) > 0) {
                                                            const currentAmount = t.amount || 0;
                                                            if (currentAmount === 0) {
                                                                updates.amount = parseFloat(typeDef.defaultAmount);
                                                            }
                                                        }
                                                    }

                                                    // Update Local State
                                                    setMonthData(prev => ({
                                                        ...prev,
                                                        transactions: prev.transactions.map(tr => tr.id === t.id ? { ...tr, ...updates } : tr)
                                                    }));

                                                    // Saving
                                                    if (newTypeId) {
                                                        // Construct safe object for save
                                                        const updatedT = { ...t, ...updates };
                                                        handleTransactionBlur(updatedT);
                                                    }
                                                }}
                                                style={{ width: '100%', border: 'none', background: 'transparent', color: 'var(--text-color)' }}
                                            >
                                                <option value="" disabled style={{ color: '#aaa' }}>Selecione...</option>
                                                {transactionTypes.map(type => (
                                                    <option key={type.id} value={type.id} style={{ color: '#000' }}>{type.description}</option>
                                                ))}
                                            </select>
                                        </td>
                                        <td style={{ padding: '5px' }}>
                                            <input
                                                type="number"
                                                value={t.day}
                                                min="1"
                                                max={new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0).getDate()}
                                                onFocus={() => setFocusedRowId(t.id)}
                                                onChange={e => handleTransactionChange(t.id, 'day', e.target.value)}
                                                onBlur={(e) => {
                                                    // Ensure day is valid integer before blurring/saving
                                                    const val = parseInt(e.target.value);
                                                    if (!isNaN(val)) {
                                                        const year = currentDate.getFullYear();
                                                        const month = currentDate.getMonth();
                                                        const daysInMonth = new Date(year, month + 1, 0).getDate();
                                                        let safeDay = val;
                                                        if (safeDay < 1) safeDay = 1;
                                                        if (safeDay > daysInMonth) safeDay = daysInMonth;

                                                        if (safeDay !== val) {
                                                            handleTransactionChange(t.id, 'day', safeDay);
                                                        }

                                                        handleTransactionBlur({ ...t, day: safeDay });
                                                    }
                                                }}
                                                style={{ width: '100%', border: 'none', textAlign: 'center', background: 'transparent', color: 'var(--text-color)' }}
                                            />
                                        </td>
                                        <td style={{ padding: '5px', textAlign: 'center' }}>
                                            {/* Icon Override Button */}
                                            <div
                                                style={{
                                                    cursor: 'pointer',
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    width: '24px',
                                                    height: '24px',
                                                    minWidth: '24px',
                                                    borderRadius: '4px',
                                                    margin: '0 auto',
                                                    // Removed orange highlight/border as per user request to match standard list style
                                                    background: 'transparent',
                                                    border: '1px solid transparent'
                                                }}
                                                onClick={() => setEditingIcon({
                                                    isOpen: true,
                                                    transactionId: t.id,
                                                    // Use transaction icon OR type icon as current
                                                    currentIcon: t.iconName || (typeDef?.iconName || '')
                                                })}
                                                title={t.iconName ? `Ícone Personalizado (Padrão: ${typeDef?.description})` : `Ícone Padrão: ${typeDef?.description}`}
                                            >
                                                {(() => {
                                                    const effectiveIcon = t.iconName || typeDef?.iconName;
                                                    const IconComp = getIcon(effectiveIcon);
                                                    return IconComp ? <IconComp size={16} color="#3498db" /> : null;
                                                })()}
                                            </div>
                                        </td>
                                        <td style={{ padding: '5px' }}>
                                            <input
                                                type="text"
                                                value={t.description}
                                                onFocus={() => setFocusedRowId(t.id)}
                                                onChange={e => handleTransactionChange(t.id, 'description', e.target.value)}
                                                onBlur={() => handleTransactionBlur(t)}
                                                style={{ width: '100%', border: 'none', background: 'transparent', color: 'var(--text-color)' }}
                                            />
                                        </td>
                                        <td style={{ padding: '5px', textAlign: 'center' }}>
                                            <span
                                                title={isIncome ? 'Entrada (Income)' : 'Saída (Expense)'}
                                                style={{
                                                    color: isIncome ? '#81c784' : '#e57373',
                                                    fontWeight: 'bold',
                                                    cursor: 'help'
                                                }}
                                            >
                                                {isIncome ? <FaPlus size={12} /> : <FaMinus size={12} />}
                                            </span>
                                        </td>
                                        <td style={{ padding: '5px' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                                                <div style={{ flex: 1 }}>
                                                    <MoneyInput
                                                        value={t.amount}
                                                        onFocus={() => setFocusedRowId(t.id)}
                                                        onSave={(newAmount) => {
                                                            handleTransactionChange(t.id, 'amount', newAmount);
                                                            // Construct updated transaction safely for save
                                                            const updatedT = { ...t, amount: newAmount };
                                                            handleTransactionBlur(updatedT);
                                                        }}
                                                        textColor="var(--text-color)"
                                                    />
                                                </div>
                                                <button
                                                    onClick={() => handleImportLastValue(t)}
                                                    onFocus={() => setFocusedRowId(t.id)}
                                                    className="btn-icon"
                                                    style={{
                                                        background: 'transparent',
                                                        border: 'none',
                                                        cursor: t.transactionTypeId ? 'pointer' : 'not-allowed',
                                                        color: t.transactionTypeId ? '#555' : '#ccc',
                                                        padding: '2px',
                                                        fontSize: '12px'
                                                    }}
                                                    title="Importar último valor usado"
                                                    disabled={!t.transactionTypeId}
                                                >
                                                    <FaHistory />
                                                </button>
                                            </div>
                                        </td>
                                        <td style={{ padding: '5px', textAlign: 'right', color: runningBalance >= 0 ? 'green' : 'red', fontSize: '14px' }}>
                                            {!isFilterActive && runningBalance.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                        </td>
                                        <td style={{ padding: '5px', textAlign: 'center' }}>
                                            <input
                                                type="checkbox"
                                                checked={t.status === 'COMPLETED'}
                                                onChange={e => {
                                                    const newStatus = e.target.checked ? 'COMPLETED' : 'PENDING';
                                                    handleTransactionChange(t.id, 'status', newStatus);
                                                    const updatedT = { ...t, status: newStatus };
                                                    handleTransactionBlur(updatedT);
                                                }}
                                                onFocus={() => setFocusedRowId(t.id)}
                                                style={{ cursor: 'pointer', transform: 'scale(1.2)' }}
                                            />
                                        </td>
                                        <td style={{ padding: '5px', textAlign: 'center' }}>
                                            <button
                                                onClick={() => setEditingRemark({ isOpen: true, transactionId: t.id, currentRemark: t.remark })}
                                                onFocus={() => setFocusedRowId(t.id)}
                                                className="btn-icon"
                                                style={{ color: hasRemark ? 'green' : '#888', cursor: 'pointer', border: 'none', background: 'transparent' }}
                                                title={t.remark || "Adicionar observação"}
                                            >
                                                {hasRemark ? <FaComment size={14} /> : <FaRegComment size={14} />}
                                            </button>
                                        </td>
                                        <td style={{ padding: '5px', textAlign: 'center' }}>
                                            <button
                                                onClick={() => handleDeleteTransaction(t.id)}
                                                onFocus={() => setFocusedRowId(t.id)}
                                                title="Excluir"
                                                style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: '#ff4444', display: 'flex', alignItems: 'center', justifyContent: 'center' }}
                                            >
                                                <FaTrash size={14} />
                                            </button>
                                        </td>
                                    </tr>
                                );
                            });
                        })()}
                    </tbody>
                    <tfoot style={{ position: 'sticky', bottom: 0, zIndex: 1, backgroundColor: '#2a2a2a', boxShadow: '0 -2px 5px rgba(0,0,0,0.5)', color: '#fff' }}>
                        <tr>
                            <td colSpan="10" style={{ padding: '10px 15px', borderTop: '1px solid #333' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <div>
                                        {(!filters.day && filters.types.length === 0) && (
                                            <button
                                                onClick={handleAddLine}
                                                style={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    gap: '5px',
                                                    padding: '8px 16px',
                                                    borderRadius: '4px',
                                                    border: '1px solid #555',
                                                    background: '#333',
                                                    cursor: 'pointer',
                                                    fontSize: '14px',
                                                    fontWeight: '500',
                                                    color: '#fff'
                                                }}
                                            >
                                                <FaPlus /> Adicionar Lançamento
                                            </button>
                                        )}
                                    </div>
                                    <div style={{ display: 'flex', gap: '5px' }}>
                                        <button
                                            onClick={handleScrollTop}
                                            title="Ir para o topo"
                                            style={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                padding: '8px',
                                                borderRadius: '4px',
                                                border: '1px solid #555',
                                                background: '#333',
                                                cursor: 'pointer',
                                                color: '#fff'
                                            }}
                                        >
                                            <FaArrowUp />
                                        </button>
                                        <button
                                            onClick={handleScrollBottom}
                                            title="Ir para o fim"
                                            style={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                padding: '8px',
                                                borderRadius: '4px',
                                                border: '1px solid #555',
                                                background: '#333',
                                                cursor: 'pointer',
                                                color: '#fff'
                                            }}
                                        >
                                            <FaArrowDown />
                                        </button>
                                        <button
                                            onClick={() => fetchMonthData(currentDate.getMonth() + 1, currentDate.getFullYear())}
                                            title="Atualizar Tabela (Sort e Saldo)"
                                            style={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                padding: '8px',
                                                borderRadius: '4px',
                                                border: '1px solid #555',
                                                background: '#333',
                                                cursor: 'pointer',
                                                color: '#fff'
                                            }}
                                        >
                                            <FaSync />
                                        </button>
                                    </div>
                                </div>
                            </td>
                        </tr>
                    </tfoot>
                </table>
            </div>

            {/* Frozen Footer */}
            <div style={{ position: 'fixed', bottom: 0, left: 0, right: 0, background: '#2c3e50', color: 'white', padding: '15px 30px', display: 'flex', justifyContent: 'center', gap: '50px', boxShadow: '0 -2px 10px rgba(0,0,0,0.1)', zIndex: 10 }}>
                <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: '0.8rem', opacity: 0.8 }}>PLANEJADO</div>
                    <div style={{ display: 'flex', gap: '15px', marginTop: '5px' }}>
                        <span style={{ color: '#81c784' }}>+ {formatCurrency(totals.plannedIncome)}</span>
                        <span style={{ color: '#e57373' }}>- {formatCurrency(totals.plannedExpense)}</span>
                        <strong style={{ marginLeft: '10px' }}>= {formatCurrency(plannedBalance)}</strong>
                    </div>
                </div>
                <div style={{ borderLeft: '1px solid rgba(255,255,255,0.2)' }}></div>
                <div style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: '0.8rem', opacity: 0.8 }}>REALIZADO</div>
                    <div style={{ display: 'flex', gap: '15px', marginTop: '5px' }}>
                        <span style={{ color: '#81c784' }}>+ {formatCurrency(totals.realizedIncome)}</span>
                        <span style={{ color: '#e57373' }}>- {formatCurrency(totals.realizedExpense)}</span>
                        <strong style={{ marginLeft: '10px' }}>= {formatCurrency(realizedBalance)}</strong>
                    </div>
                </div>
            </div>

            <MessageModal
                isOpen={messageModal.isOpen}
                onClose={() => setMessageModal({ ...messageModal, isOpen: false })}
                title={messageModal.title}
                message={messageModal.message}
                type={messageModal.type}
            />

            <ConfirmationModal
                isOpen={confirmModal.isOpen}
                onClose={() => setConfirmModal({ isOpen: false, transactionId: null })}
                onConfirm={confirmDeleteTransaction}
                title="Confirmar Exclusão"
                message="Tem certeza que deseja excluir esta transação?"
                confirmText="Sim, Excluir"
                cancelText="Cancelar"
            />

            {/* Icon Override Modal */}
            {editingIcon.isOpen && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
                }}>
                    <div style={{ background: 'white', padding: '15px', borderRadius: '8px', width: '350px', boxShadow: '0 2px 10px rgba(0,0,0,0.2)' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                            <h3 style={{ margin: 0, fontSize: '16px' }}>Selecionar Ícone</h3>
                            {editingIcon.currentIcon && (
                                <button
                                    onClick={() => handleIconSave(null)} // Clear icon to revert to default
                                    style={{ background: 'transparent', border: 'none', color: 'red', fontSize: '12px', cursor: 'pointer', textDecoration: 'underline' }}
                                >
                                    Restaurar Padrão
                                </button>
                            )}
                        </div>

                        <IconSelector
                            selectedIcon={editingIcon.currentIcon}
                            onSelect={(newIcon) => handleIconSave(newIcon)}
                        />

                        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '15px' }}>
                            <button
                                onClick={() => setEditingIcon({ isOpen: false, transactionId: null, currentIcon: '' })}
                                style={{ padding: '6px 12px', background: '#ccc', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                            >
                                Cancelar
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <RemarkModal
                isOpen={editingRemark.isOpen}
                onClose={() => setEditingRemark({ ...editingRemark, isOpen: false })}
                initialValue={editingRemark.currentRemark}
                onSave={handleRemarkSave}
            />
        </div >
    );
}

const formatDecimal = (value) => {
    if (value === undefined || value === null) return '0,00';
    return value.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

const MoneyInput = ({ value, onSave, onFocus, textColor }) => {
    // We maintain internal state to handle the shifting logic smoothly
    // and only trigger onSave (which calls API) on blur.
    const [internalValue, setInternalValue] = useState(0);

    useEffect(() => {
        let val = 0;
        if (value !== undefined && value !== null) {
            val = typeof value === 'string' ? parseFloat(value) : value;
        }
        setInternalValue(val || 0);
    }, [value]);

    const handleChange = (e) => {
        const raw = e.target.value.replace(/\D/g, '');
        const val = raw ? parseFloat(raw) / 100 : 0;
        setInternalValue(val);
    };

    const handleBlur = () => {
        if (onSave) {
            // Check if value actually changed to facilitate change detection/avoid unnecessary saves
            // Note: value prop might be string or number
            let parentVal = 0;
            if (value !== undefined && value !== null) {
                parentVal = typeof value === 'string' ? parseFloat(value) : value;
            }

            // Allow small float diff or strict? 
            if (Math.abs((parentVal || 0) - internalValue) > 0.001) {
                onSave(internalValue);
            }
        }
    };

    const handleFocusInternal = (e) => {
        if (onFocus) onFocus(e);
        // Optional: Select all on focus for easier replacement
        // e.target.select(); 
    };

    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            e.target.blur();
        }
    };

    const formattedValue = (typeof internalValue === 'number' ? internalValue : 0)
        .toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

    return (
        <input
            type="text"
            value={formattedValue}
            onChange={handleChange}
            onBlur={handleBlur}
            onFocus={handleFocusInternal}
            onKeyDown={handleKeyDown}
            style={{
                width: '100%',
                textAlign: 'right',
                border: 'none',
                background: 'transparent',
                // outline: 'none', // Removed to match other inputs which show focus outline
                fontSize: '14px',
                color: textColor || 'inherit'
            }}
        />
    );
};

const RemarkModal = ({ isOpen, onClose, initialValue, onSave }) => {
    const [value, setValue] = useState(initialValue || '');

    useEffect(() => {
        setValue(initialValue || '');
    }, [initialValue, isOpen]);

    if (!isOpen) return null;

    return (
        <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
        }}>
            <div style={{ background: 'white', padding: '20px', borderRadius: '8px', width: '400px', boxShadow: '0 2px 10px rgba(0,0,0,0.2)' }}>
                <h3 style={{ marginTop: 0, color: '#333' }}>Observação</h3>
                <textarea
                    value={value}
                    onChange={(e) => setValue(e.target.value)}
                    style={{ width: '100%', height: '100px', padding: '10px', marginTop: '10px', borderRadius: '4px', border: '1px solid #ccc', resize: 'vertical', boxSizing: 'border-box', color: '#333' }}
                    placeholder="Digite uma observação..."
                />
                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                    <button onClick={onClose} style={{ padding: '8px 16px', background: '#ccc', border: 'none', borderRadius: '4px', cursor: 'pointer' }}>Cancelar</button>
                    <button
                        onClick={() => { onSave(value); onClose(); }}
                        style={{ padding: '8px 16px', background: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                    >
                        Salvar
                    </button>
                </div>
            </div>
        </div>
    );
};
