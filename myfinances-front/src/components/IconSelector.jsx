import { useState, useEffect } from 'react';
import { ICON_GROUPS, getIcon, getGroupForIcon } from '../utils/IconRepository';

export function IconSelector({ selectedIcon, onSelect, readOnly = false }) {
    // Determine initial group based on the selectedIcon (if any)
    const [selectedGroup, setSelectedGroup] = useState('Diversos');

    // Sync internal group state if selectedIcon changes externally (e.g. edit mode load)
    useEffect(() => {
        if (selectedIcon) {
            const group = getGroupForIcon(selectedIcon);
            if (group) {
                setSelectedGroup(group);
            }
        }
    }, [selectedIcon]);

    if (readOnly) {
        const IconComponent = getIcon(selectedIcon);
        return IconComponent ? <IconComponent size={24} /> : <span>-</span>;
    }

    const availableIcons = ICON_GROUPS[selectedGroup] || [];

    return (
        <div style={{ padding: '10px', border: '1px solid #ddd', borderRadius: '8px', background: '#f9f9f9' }}>
            <div style={{ marginBottom: '10px' }}>
                <label style={{ fontSize: '12px', fontWeight: 'bold' }}>Categoria do Ícone:</label>
                <select
                    value={selectedGroup}
                    onChange={(e) => setSelectedGroup(e.target.value)}
                    style={{ width: '100%', padding: '5px', marginTop: '5px' }}
                >
                    {Object.keys(ICON_GROUPS).map(group => (
                        <option key={group} value={group}>{group}</option>
                    ))}
                </select>
            </div>

            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', maxHeight: '150px', overflowY: 'auto' }}>
                {availableIcons.map(iconName => {
                    const IconComponent = getIcon(iconName);
                    if (!IconComponent) return null;

                    const isSelected = selectedIcon === iconName;

                    return (
                        <div
                            key={iconName}
                            onClick={() => onSelect(iconName)}
                            title={iconName}
                            style={{
                                cursor: 'pointer',
                                padding: '8px',
                                border: isSelected ? '2px solid #3498db' : '1px solid #ccc',
                                borderRadius: '4px',
                                background: isSelected ? '#e1f5fe' : 'white',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                width: '40px',
                                height: '40px'
                            }}
                        >
                            <IconComponent size={20} color={isSelected ? '#3498db' : '#3498db'} />
                        </div>
                    );
                })}
            </div>
            {selectedIcon && (
                <div style={{ marginTop: '5px', fontSize: '12px', color: '#666', textAlign: 'right' }}>
                    Selecionado: {selectedIcon}
                </div>
            )}
        </div>
    );
}
