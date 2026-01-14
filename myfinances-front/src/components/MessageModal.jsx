import React from 'react';

export const MessageModal = ({ isOpen, onClose, title, message, type = 'info' }) => {
    if (!isOpen) return null;

    const isError = type === 'error';
    const buttonColor = isError ? '#d9534f' : '#007bff';

    return (
        <div style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            zIndex: 1000
        }}>
            <div style={{
                background: 'white',
                padding: '20px',
                borderRadius: '8px',
                width: '400px',
                maxWidth: '90%',
                boxShadow: '0 4px 6px rgba(0,0,0,0.1)'
            }}>
                <h3 style={{ marginTop: 0, marginBottom: '15px', color: isError ? '#d9534f' : '#333' }}>
                    {title}
                </h3>
                <p style={{ color: '#666', marginBottom: '25px', whiteSpace: 'pre-wrap' }}>{message}</p>
                <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <button
                        onClick={onClose}
                        style={{
                            padding: '8px 24px',
                            background: buttonColor,
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer',
                            fontWeight: 'bold'
                        }}
                    >
                        OK
                    </button>
                </div>
            </div>
        </div>
    );
};
