import pdfMake from "pdfmake/build/pdfmake";
import pdfFonts from "pdfmake/build/vfs_fonts";
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { getIcon } from '../utils/IconRepository';

pdfMake.vfs = pdfFonts.pdfMake ? pdfFonts.pdfMake.vfs : pdfFonts.vfs;

const formatNumber = (value) => {
    return new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value || 0);
};

export const generateMonthlyReport = (reportData, transactionTypes) => {
    const { month, year, initialBalance, transactions } = reportData;

    let plannedIncome = 0;
    let plannedExpense = 0;
    let realizedIncome = 0;
    let realizedExpense = 0;

    const tableBody = [];
    
    // Header Row
    tableBody.push([
        { text: 'Dia', style: 'tableHeader' },
        { text: 'Tipo', style: 'tableHeader' },
        { text: 'Descrição', style: 'tableHeader' },
        { text: 'Valor (R$)', style: 'tableHeader', alignment: 'right' },
        { text: 'Status', style: 'tableHeader', alignment: 'center' },
        { text: 'Observação', style: 'tableHeader' }
    ]);

    transactions.forEach(t => {
        const typeDef = transactionTypes.find(type => type.id === t.transactionTypeId);
        const isIncome = typeDef?.type === 'INCOME';

        const IconComponent = getIcon(typeDef?.iconName);
        let svgString = null;
        if (IconComponent) {
            svgString = renderToStaticMarkup(React.createElement(IconComponent));
            svgString = svgString.replace(/currentColor/g, '#000000');
        }

        // Planned (All filtered transactions)
        if (isIncome) plannedIncome += t.amount || 0;
        else plannedExpense += t.amount || 0;

        // Realized
        if (t.status === 'COMPLETED') {
            if (isIncome) realizedIncome += t.amount || 0;
            else realizedExpense += t.amount || 0;
        }

        const statusStr = t.status === 'COMPLETED' ? 'Realizado' : 'Pendente';
        const amountColor = isIncome ? 'green' : 'red';

        // Main transaction row
        tableBody.push([
            t.day.toString(),
            {
                columns: [
                    svgString ? { svg: svgString, width: 10, margin: [0, 2, 0, 0] } : { text: '', width: 10 },
                    { text: t.transactionTypeDescription || '', width: '*' }
                ],
                columnGap: 5
            },
            t.description || '',
            { text: formatNumber(t.amount), color: amountColor, alignment: 'right' },
            { text: statusStr, alignment: 'center' },
            { text: t.remark || '', italics: true, style: 'detailText' }
        ]);

        // Details rows
        if (t.details && t.details.length > 0) {
            tableBody.push([
                { text: '', style: 'detailText' }, // Dia empty
                { text: '', style: 'detailText' }, // Tipo empty
                { text: 'Detalhes do lançamento:', style: 'detailText', italics: true },
                { text: '', style: 'detailText' }, // Valor empty
                { text: '', style: 'detailText' }, // Status empty
                { text: '', style: 'detailText' }  // Observação empty
            ]);

            t.details.forEach(d => {
                const dateObj = new Date(d.detailDate);
                const dayStr = dateObj.getUTCDate().toString();
                tableBody.push([
                    { text: '', style: 'detailText' }, // Dia empty (keeps style for border hiding)
                    { text: '', style: 'detailText' }, // Tipo empty
                    {
                        columns: [
                            { text: dayStr, width: 15, alignment: 'right', style: 'detailText' },
                            { text: `  ↳ ${d.description}`, width: '*', alignment: 'left', style: 'detailText' },
                            { text: formatNumber(d.amount), width: 'auto', alignment: 'right', style: 'detailText' }
                        ],
                        columnGap: 5
                    },
                    { text: '', style: 'detailText' }, // Valor empty
                    { text: '', style: 'detailText' }, // Status empty
                    { text: '', style: 'detailText' }  // Observação empty
                ]);
            });
        }
    });

    const plannedBalance = initialBalance + plannedIncome - plannedExpense;
    const realizedBalance = initialBalance + realizedIncome - realizedExpense;

    const docDefinition = {
        pageOrientation: 'landscape',
        footer: function(currentPage, pageCount) {
            return {
                text: `Página: ${currentPage} / ${pageCount}`,
                alignment: 'right',
                italics: true,
                margin: [0, 10, 20, 0],
                fontSize: 10
            };
        },
        content: [
            { text: `Relatório de Lançamentos - ${month.toString().padStart(2, '0')}/${year}`, style: 'header' },
            { text: `Saldo Inicial (R$): ${formatNumber(initialBalance)}`, style: 'subheader', margin: [0, 0, 0, 10] },
            
            {
                table: {
                    headerRows: 1,
                    widths: ['auto', 'auto', '*', 'auto', 'auto', '*'],
                    body: tableBody
                },
                layout: {
                    hLineWidth: function (i, node) {
                        if (i === 0 || i === node.table.body.length) return 2;
                        const rowBelow = node.table.body[i];
                        if (rowBelow && rowBelow[0] && rowBelow[0].style === 'detailText') {
                            return 0;
                        }
                        return 1;
                    },
                    vLineWidth: function (i, node) {
                        return 0;
                    },
                    hLineColor: function (i, node) {
                        return (i === 0 || i === node.table.body.length) ? 'black' : 'gray';
                    },
                    paddingLeft: function (i, node) { return 4; },
                    paddingRight: function (i, node) { return 4; }
                }
            },
            
            { text: 'Resumo', style: 'header', margin: [0, 20, 0, 10] },
            {
                table: {
                    widths: ['*', '*', '*'],
                    body: [
                        [
                            { text: 'Planejado', style: 'tableHeader' },
                            { text: 'Realizado', style: 'tableHeader' },
                            { text: 'Saldo Final (R$)', style: 'tableHeader', alignment: 'center' }
                        ],
                        [
                            {
                                text: [
                                    { text: 'Receitas (R$): ', color: 'black' },
                                    { text: formatNumber(plannedIncome) + '\n', color: plannedIncome < 0 ? 'red' : 'black' },
                                    { text: 'Despesas (R$): ', color: 'black' },
                                    { text: formatNumber(plannedExpense) + '\n', color: plannedExpense < 0 ? 'red' : 'black' },
                                    { text: 'Saldo (R$): ', color: 'black' },
                                    { text: formatNumber(plannedBalance), color: plannedBalance < 0 ? 'red' : 'black' }
                                ]
                            },
                            {
                                text: [
                                    { text: 'Receitas (R$): ', color: 'black' },
                                    { text: formatNumber(realizedIncome) + '\n', color: realizedIncome < 0 ? 'red' : 'black' },
                                    { text: 'Despesas (R$): ', color: 'black' },
                                    { text: formatNumber(realizedExpense) + '\n', color: realizedExpense < 0 ? 'red' : 'black' },
                                    { text: 'Saldo (R$): ', color: 'black' },
                                    { text: formatNumber(realizedBalance), color: realizedBalance < 0 ? 'red' : 'black' }
                                ]
                            },
                            { text: formatNumber(realizedBalance), fontSize: 16, bold: true, alignment: 'center', margin: [0, 10, 0, 0], color: realizedBalance < 0 ? 'red' : 'black' }
                        ]
                    ]
                }
            }
        ],
        styles: {
            header: {
                fontSize: 18,
                bold: true,
                margin: [0, 0, 0, 10]
            },
            subheader: {
                fontSize: 14,
                bold: true
            },
            tableHeader: {
                bold: true,
                fontSize: 13,
                color: 'black'
            },
            detailText: {
                fontSize: 10,
                color: '#555'
            }
        }
    };

    pdfMake.createPdf(docDefinition).open();
};
