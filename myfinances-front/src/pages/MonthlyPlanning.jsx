import { useState, useEffect } from 'react';
import { api } from '../services/api';
import { usePageTitle } from '../context/PageTitleContext';
import { FaPlus, FaMinus, FaTrash, FaChevronLeft, FaChevronRight, FaRegComment, FaComment, FaHistory } from 'react-icons/fa';
import { MessageModal } from '../components/MessageModal';
import { ConfirmationModal } from '../components/ConfirmationModal';

export function MonthlyPlanning() {
    const { setTitle } = usePageTitle();
    const [currentDate, setCurrentDate] = useState(new Date());
    const [monthData, setMonthData] = useState(null);
    const [transactionTypes, setTransactionTypes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [messageModal, setMessageModal] = useState({ isOpen: false, title: '', message: '', type: 'info' });
    const [editingRemark, setEditingRemark] = useState({ isOpen: false, transactionId: null, currentRemark: '' });

    // Confirmation Modal state
    const [confirmModal, setConfirmModal] = useState({ isOpen: false, transactionId: null });

    const handleRemarkSave = async (remark) => {
        if (!editingRemark.transactionId) return;

        // Optimistic update
        const updatedTransactions = monthData.transactions.map(t =>
            t.id === editingRemark.transactionId ? { ...t, remark: remark } : t
        );
        const targetTransaction = updatedTransactions.find(t => t.id === editingRemark.transactionId);

        setMonthData(prev => ({ ...prev, transactions: updatedTransactions }));

        // Save to backend
        handleTransactionBlur(targetTransaction);
        setEditingRemark({ isOpen: false, transactionId: null, currentRemark: '' });
    };

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

    const handleInitialBalanceChange = async (e) => {
        const value = e.target.value;
        // Optimistic update
        setMonthData(prev => ({ ...prev, initialBalance: value }));
    };

    const handleInitialBalanceBlur = async () => {
        try {
            await api.patch(`/transaction-months/${monthData.id}/initial-balance`, monthData.initialBalance, {
                headers: { 'Content-Type': 'application/json' }
            });
            fetchMonthData(currentDate.getMonth() + 1, currentDate.getFullYear()); // Refresh for calc
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
    };

    const handleTransactionChange = (id, field, value) => {
        setMonthData(prev => ({
            ...prev,
            transactions: prev.transactions.map(t => t.id === id ? { ...t, [field]: value } : t)
        }));
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
                // When we get response, the new transaction is in the list with a real ID.
                // We need to remove the temp one we just saved from our local "temps" list to avoid duplication/retention,
                // BUT mergeWithTemps logic blindly re-adds all temps.
                // So we need a smarter merge or just filter this specific temp ID out before merging.

                setMonthData(prev => {
                    const remainingTemps = prev.transactions.filter(t => isTempId(t.id) && t.id !== transaction.id);
                    return {
                        ...response.data,
                        transactions: [...response.data.transactions, ...remainingTemps]
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
                setMonthData(prev => mergeWithTemps(response.data, prev.transactions));
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

        let plannedIncome = 0, plannedExpense = 0, realizedIncome = 0, realizedExpense = 0;

        monthData.transactions.forEach(t => {
            const type = transactionTypes.find(type => type.id === t.transactionTypeId);
            const isIncome = type?.type === 'INCOME';

            // Planned (All)
            if (isIncome) plannedIncome += t.amount || 0;
            else plannedExpense += t.amount || 0;

            // Realized (Paid/Received)
            if (['PAID', 'RECEIVED'].includes(t.status)) {
                if (isIncome) realizedIncome += t.amount || 0;
                else realizedExpense += t.amount || 0;
            }
        });

        return { plannedIncome, plannedExpense, realizedIncome, realizedExpense };
    };

    const totals = calculateTotals();
    const plannedBalance = (parseFloat(monthData?.initialBalance) || 0) + totals.plannedIncome - totals.plannedExpense;
    const realizedBalance = (parseFloat(monthData?.initialBalance) || 0) + totals.realizedIncome - totals.realizedExpense;

    if (loading && !monthData) return <div style={{ padding: '20px', textAlign: 'center' }}>Carregando...</div>;



    return (
        <div style={{ display: 'flex', flexDirection: 'column', height: 'calc(100vh - 80px)', maxWidth: '1200px', margin: '0 auto', padding: '10px' }}>

            {/* Header: Select Month & Initial Balance */}
            <div className="card" style={{ marginBottom: '15px', padding: '15px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
                    <button onClick={() => changeMonth(-1)} className="btn-icon"><FaChevronLeft /></button>
                    <h2 style={{ margin: 0, textTransform: 'capitalize', width: '200px', textAlign: 'center' }}>
                        {currentDate.toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' })}
                    </h2>
                    <button onClick={() => changeMonth(1)} className="btn-icon"><FaChevronRight /></button>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <label>Saldo Inicial:</label>
                    <input
                        type="number"
                        value={monthData?.initialBalance || 0}
                        onChange={handleInitialBalanceChange}
                        onBlur={handleInitialBalanceBlur}
                        style={{ padding: '8px', borderRadius: '4px', border: '1px solid #ccc', width: '120px', textAlign: 'right' }}
                    />
                </div>
            </div>

            {/* Spreadsheet Table */}
            <div style={{ flex: 1, overflowY: 'auto', border: '1px solid #ddd', borderRadius: '8px', background: 'white', marginBottom: '80px' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', tableLayout: 'fixed' }}>
                    <thead style={{ position: 'sticky', top: 0, background: '#f4f4f4', zIndex: 1, color: '#333' }}>
                        <tr>
                            <th style={{ width: '200px', padding: '10px', borderBottom: '1px solid #ddd' }}>Categ.</th>
                            <th style={{ width: '60px', padding: '10px', borderBottom: '1px solid #ddd' }}>Dia</th>
                            <th style={{ width: '40px', padding: '10px', borderBottom: '1px solid #ddd', textAlign: 'center' }}>T</th>
                            <th style={{ padding: '10px', borderBottom: '1px solid #ddd' }}>Descrição</th>
                            <th style={{ width: '120px', padding: '10px', borderBottom: '1px solid #ddd' }}>Valor</th>
                            <th style={{ width: '120px', padding: '10px', borderBottom: '1px solid #ddd' }}>Saldo</th>
                            <th style={{ width: '120px', padding: '10px', borderBottom: '1px solid #ddd' }}>Status</th>
                            <th style={{ width: '40px', padding: '10px', borderBottom: '1px solid #ddd' }}>Obs</th>
                            <th style={{ width: '50px', padding: '10px', borderBottom: '1px solid #ddd' }}>#</th>
                        </tr>
                    </thead>
                    <tbody>
                        {(() => {
                            let runningBalance = parseFloat(monthData?.initialBalance || 0);
                            return monthData?.transactions.map(t => {
                                const typeDef = transactionTypes.find(type => type.id === t.transactionTypeId);
                                const isIncome = typeDef?.type === 'INCOME';
                                const amount = t.amount || 0;

                                if (isIncome) runningBalance += amount;
                                else runningBalance -= amount;

                                const hasRemark = t.remark && t.remark.trim().length > 0;

                                return (
                                    <tr key={t.id} style={{ borderBottom: '1px solid #eee' }}>
                                        <td style={{ padding: '5px' }}>
                                            <select
                                                value={t.transactionTypeId || ''}
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
                                                style={{ width: '100%', border: 'none', background: 'transparent' }}
                                            >
                                                <option value="" disabled>Selecione...</option>
                                                {transactionTypes.map(type => (
                                                    <option key={type.id} value={type.id}>{type.description}</option>
                                                ))}
                                            </select>
                                        </td>
                                        <td style={{ padding: '5px' }}>
                                            <input
                                                type="number"
                                                value={t.day}
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
                                                style={{ width: '100%', border: 'none', textAlign: 'center' }}
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
                                            <input
                                                type="text"
                                                value={t.description}
                                                onChange={e => handleTransactionChange(t.id, 'description', e.target.value)}
                                                onBlur={() => handleTransactionBlur(t)}
                                                style={{ width: '100%', border: 'none' }}
                                            />
                                        </td>
                                        <td style={{ padding: '5px' }}>
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                                                <div style={{ flex: 1 }}>
                                                    <MoneyInput
                                                        value={t.amount}
                                                        onSave={(newAmount) => {
                                                            handleTransactionChange(t.id, 'amount', newAmount);
                                                            // Construct updated transaction safely for save
                                                            const updatedT = { ...t, amount: newAmount };
                                                            handleTransactionBlur(updatedT);
                                                        }}
                                                    />
                                                </div>
                                                <button
                                                    onClick={() => handleImportLastValue(t)}
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
                                            {runningBalance.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                        </td>
                                        <td style={{ padding: '5px' }}>
                                            <select
                                                value={t.status}
                                                onChange={e => {
                                                    handleTransactionChange(t.id, 'status', e.target.value);
                                                    const updatedT = { ...t, status: e.target.value };
                                                    handleTransactionBlur(updatedT);
                                                }}
                                                style={{
                                                    width: '100%', border: 'none', background: 'transparent',
                                                    color: ['PAID', 'RECEIVED'].includes(t.status) ? 'green' : 'orange'
                                                }}
                                            >
                                                <option value="PENDING">Pendente</option>
                                                <option value="PAID">Pago</option>
                                                <option value="RECEIVED">Realizado</option>
                                            </select>
                                        </td>
                                        <td style={{ padding: '5px', textAlign: 'center' }}>
                                            <button
                                                onClick={() => setEditingRemark({ isOpen: true, transactionId: t.id, currentRemark: t.remark })}
                                                className="btn-icon"
                                                style={{ color: hasRemark ? 'green' : 'black', cursor: 'pointer', border: 'none', background: 'transparent' }}
                                                title={t.remark || "Adicionar observação"}
                                            >
                                                {hasRemark ? <FaComment size={14} /> : <FaRegComment size={14} />}
                                            </button>
                                        </td>
                                        <td style={{ padding: '5px', textAlign: 'center' }}>
                                            <button
                                                onClick={() => handleDeleteTransaction(t.id)}
                                                className="btn-icon"
                                                style={{ color: '#ccc', cursor: 'pointer' }}
                                                onMouseEnter={e => e.target.style.color = 'red'}
                                                onMouseLeave={e => e.target.style.color = '#ccc'}
                                            >
                                                <FaTrash size={14} />
                                            </button>
                                        </td>
                                    </tr>
                                );
                            });
                        })()}
                        <tr onClick={handleAddLine} style={{ cursor: 'pointer', background: '#fdfdfd' }}>
                            <td colSpan="9" style={{ padding: '10px', textAlign: 'center', color: '#888', borderTop: '1px dashed #ddd' }}>
                                <FaPlus style={{ marginRight: '5px' }} /> Adicionar Linha
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            {/* Frozen Footer */}
            <div style={{ position: 'fixed', bottom: 0, left: 0, right: 0, background: '#2c3e50', color: 'white', padding: '15px 30px', display: 'flex', justifyContent: 'center', gap: '50px', boxShadow: '0 -2px 10px rgba(0,0,0,0.1)' }}>
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

            <RemarkModal
                isOpen={editingRemark.isOpen}
                onClose={() => setEditingRemark({ ...editingRemark, isOpen: false })}
                initialValue={editingRemark.currentRemark}
                onSave={handleRemarkSave}
            />
        </div>
    );
}

const formatDecimal = (value) => {
    if (value === undefined || value === null) return '0,00';
    return value.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
};

const MoneyInput = ({ value, onSave }) => {
    // Initial state based on value
    const [text, setText] = useState(formatDecimal(value));
    const [isEditing, setIsEditing] = useState(false);

    // Sync if external value changes significantly (and not editing)
    useEffect(() => {
        if (!isEditing) {
            setText(formatDecimal(value));
        }
    }, [value, isEditing]);

    const handleChange = (e) => {
        // Allow digits, commas, dots
        const raw = e.target.value;
        // Basic filtering could be done here if needed
        setText(raw);
    };

    const handleFocus = (e) => {
        setIsEditing(true);
        e.target.select();
    };

    const handleBlur = () => {
        setIsEditing(false);
        // Parse "1.234,56" or "1234.56"
        // 1. Remove thousands separator (.)
        // 2. Replace decimal separator (,) with (.)
        let clean = text.replace(/\./g, '').replace(',', '.');

        // Edge case: empty
        if (!clean) clean = '0';

        let num = parseFloat(clean);
        if (isNaN(num)) num = 0;

        // Re-format text to look nice immediately (optional, useEffect handles it too but async)
        setText(formatDecimal(num));

        onSave(num);
    };

    // Handle Enter key
    const handleKeyDown = (e) => {
        if (e.key === 'Enter') {
            e.target.blur();
        }
    };

    return (
        <input
            type="text"
            value={text}
            onChange={handleChange}
            onBlur={handleBlur}
            onFocus={handleFocus}
            onKeyDown={handleKeyDown}
            style={{ width: '100%', border: 'none', textAlign: 'right', outline: 'none', fontSize: '14px' }}
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
