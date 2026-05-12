import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { generateMonthlyReport } from '../services/TransactionReportGenerator';

export const TransactionReportFilterModal = ({ isOpen, onClose, onSuccess }) => {
    const [month, setMonth] = useState(new Date().getMonth() + 1);
    const [year, setYear] = useState(new Date().getFullYear());
    const [transactionTypeId, setTransactionTypeId] = useState('');
    const [transactionTypes, setTransactionTypes] = useState([]);
    const [planned, setPlanned] = useState(true);
    const [realized, setRealized] = useState(true);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (isOpen) {
            fetchTransactionTypes();
        }
    }, [isOpen]);

    const fetchTransactionTypes = async () => {
        try {
            const res = await api.get('/transaction-types', { params: { size: 100 } });
            setTransactionTypes(res.data.content || []);
        } catch (error) {
            console.error('Erro ao buscar tipos de transação:', error);
        }
    };

    const handleGenerate = async () => {
        setLoading(true);
        try {
            const res = await api.get(`/transaction-months/${year}/${month}/report`);
            const reportData = res.data;
            
            // Filter by transaction type
            let filteredTransactions = reportData.transactions;
            if (transactionTypeId) {
                filteredTransactions = filteredTransactions.filter(t => t.transactionTypeId === parseInt(transactionTypeId));
            }
            
            // Filter by status (Planned/Realized)
            filteredTransactions = filteredTransactions.filter(t => {
                if (t.status === 'COMPLETED' && realized) return true;
                if (t.status === 'PENDING' && planned) return true;
                return false;
            });
            
            reportData.transactions = filteredTransactions;
            
            generateMonthlyReport(reportData, transactionTypes);
            
            onClose();
            if (onSuccess) {
                onSuccess();
            }
        } catch (error) {
            console.error('Erro ao gerar relatório:', error);
            alert('Erro ao gerar relatório.');
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.7)', display: 'flex',
            justifyContent: 'center', alignItems: 'center', zIndex: 1000
        }}>
            <div style={{
                background: '#2a2a2a', color: '#fff', padding: '20px',
                borderRadius: '8px', width: '400px', maxWidth: '95%',
                boxShadow: '0 4px 6px rgba(0,0,0,0.3)',
                display: 'flex', flexDirection: 'column', gap: '15px'
            }}>
                <h3 style={{ margin: 0 }}>Lançamentos por Mês</h3>
                
                <div style={{ display: 'flex', gap: '10px' }}>
                    <div style={{ flex: 1 }}>
                        <label style={{ display: 'block', marginBottom: '5px' }}>Mês</label>
                        <select
                            value={month}
                            onChange={e => setMonth(parseInt(e.target.value))}
                            style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ccc', color: '#000' }}
                        >
                            {[...Array(12).keys()].map(i => (
                                <option key={i+1} value={i+1}>{i+1}</option>
                            ))}
                        </select>
                    </div>
                    <div style={{ flex: 1 }}>
                        <label style={{ display: 'block', marginBottom: '5px' }}>Ano</label>
                        <input
                            type="number"
                            value={year}
                            onChange={e => setYear(parseInt(e.target.value))}
                            style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ccc', boxSizing: 'border-box', color: '#000' }}
                        />
                    </div>
                </div>

                <div>
                    <label style={{ display: 'block', marginBottom: '5px' }}>Tipo de Lançamento</label>
                    <select
                        value={transactionTypeId}
                        onChange={e => setTransactionTypeId(e.target.value)}
                        style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #ccc', color: '#000' }}
                    >
                        <option value="">Todos</option>
                        {transactionTypes.map(t => (
                            <option key={t.id} value={t.id}>{t.description}</option>
                        ))}
                    </select>
                </div>

                <div style={{ display: 'flex', gap: '15px', marginTop: '10px' }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                        <input type="checkbox" checked={planned} onChange={e => setPlanned(e.target.checked)} />
                        Planejado
                    </label>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                        <input type="checkbox" checked={realized} onChange={e => setRealized(e.target.checked)} />
                        Realizado
                    </label>
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                    <button onClick={onClose} style={{ padding: '8px 16px', background: '#ccc', border: 'none', borderRadius: '4px', cursor: 'pointer', color: '#000' }}>
                        Cancelar
                    </button>
                    <button 
                        onClick={handleGenerate} 
                        disabled={loading}
                        style={{ padding: '8px 16px', background: '#3498db', color: 'white', border: 'none', borderRadius: '4px', cursor: loading ? 'not-allowed' : 'pointer' }}
                    >
                        {loading ? 'Gerando...' : 'Gerar'}
                    </button>
                </div>
            </div>
        </div>
    );
};
