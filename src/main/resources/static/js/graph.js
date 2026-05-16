const dataElement = document.getElementById('triples-data');
const triples = JSON.parse(dataElement.textContent || '[]');
const graphArea = document.getElementById('graphArea');

const nodeNames = [];
const nodeIndex = new Map();

function addNode(name) {
    if (!nodeIndex.has(name)) {
        nodeIndex.set(name, nodeNames.length);
        nodeNames.push(name);
    }
}

triples.forEach(t => {
    addNode(t.subject);
    addNode(t.object);
});

const width = Math.max(900, nodeNames.length * 85);
const height = Math.max(600, nodeNames.length * 45);
const centerX = width / 2;
const centerY = height / 2;
const radius = Math.min(width, height) / 2 - 90;

const positions = new Map();
nodeNames.forEach((name, i) => {
    const angle = (2 * Math.PI * i) / nodeNames.length;
    positions.set(name, {
        x: centerX + radius * Math.cos(angle),
        y: centerY + radius * Math.sin(angle)
    });
});

const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
svg.setAttribute('width', width);
svg.setAttribute('height', height);
svg.setAttribute('viewBox', `0 0 ${width} ${height}`);

const defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');
defs.innerHTML = `
    <marker id="arrow" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto" markerUnits="strokeWidth">
        <path d="M0,0 L0,6 L9,3 z" fill="#6b7280"></path>
    </marker>
`;
svg.appendChild(defs);

triples.forEach(t => {
    const from = positions.get(t.subject);
    const to = positions.get(t.object);
    if (!from || !to) return;

    const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
    line.setAttribute('x1', from.x);
    line.setAttribute('y1', from.y);
    line.setAttribute('x2', to.x);
    line.setAttribute('y2', to.y);
    line.setAttribute('stroke', '#9ca3af');
    line.setAttribute('stroke-width', '1.4');
    line.setAttribute('marker-end', 'url(#arrow)');
    svg.appendChild(line);

    const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
    label.setAttribute('x', (from.x + to.x) / 2);
    label.setAttribute('y', (from.y + to.y) / 2);
    label.setAttribute('class', 'edge-label');
    label.textContent = t.predicate;
    svg.appendChild(label);
});

nodeNames.forEach(name => {
    const pos = positions.get(name);

    const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
    circle.setAttribute('cx', pos.x);
    circle.setAttribute('cy', pos.y);
    circle.setAttribute('r', 28);
    circle.setAttribute('fill', '#e0f2fe');
    circle.setAttribute('stroke', '#0369a1');
    circle.setAttribute('stroke-width', '2');
    svg.appendChild(circle);

    const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
    label.setAttribute('x', pos.x);
    label.setAttribute('y', pos.y + 45);
    label.setAttribute('text-anchor', 'middle');
    label.setAttribute('class', 'node-label');
    label.textContent = name.length > 26 ? name.substring(0, 23) + '...' : name;
    svg.appendChild(label);
});

graphArea.appendChild(svg);
