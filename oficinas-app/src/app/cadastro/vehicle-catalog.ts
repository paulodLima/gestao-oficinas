export interface VehicleBrandCatalog {
  readonly name: string;
  readonly models: readonly string[];
}

/** Marcas e modelos mais frequentes nas oficinas brasileiras. */
export const VEHICLE_BRANDS: readonly VehicleBrandCatalog[] = [
  { name: 'Audi', models: ['A3', 'A4', 'A5', 'Q3', 'Q5', 'Q7'] },
  { name: 'BMW', models: ['118i', '320i', 'X1', 'X3', 'X4', 'X5'] },
  { name: 'BYD', models: ['Dolphin', 'Dolphin Mini', 'King', 'Song Plus', 'Yuan Plus'] },
  { name: 'Caoa Chery', models: ['Arrizo 6', 'Tiggo 5X', 'Tiggo 7', 'Tiggo 8'] },
  { name: 'Chevrolet', models: ['Cobalt', 'Cruze', 'Montana', 'Onix', 'Onix Plus', 'Prisma', 'S10', 'Spin', 'Tracker'] },
  { name: 'Citroën', models: ['Aircross', 'Basalt', 'C3', 'C4 Cactus', 'Jumpy'] },
  { name: 'Fiat', models: ['Argo', 'Cronos', 'Fastback', 'Fiorino', 'Mobi', 'Pulse', 'Strada', 'Toro', 'Uno'] },
  { name: 'Ford', models: ['EcoSport', 'Fiesta', 'Focus', 'Ka', 'Maverick', 'Ranger', 'Territory'] },
  { name: 'GWM', models: ['Haval H6', 'Haval H6 GT', 'Ora 03', 'Poer', 'Tank 300'] },
  { name: 'Honda', models: ['City', 'Civic', 'CR-V', 'Fit', 'HR-V', 'WR-V'] },
  { name: 'Hyundai', models: ['Creta', 'HB20', 'HB20S', 'i30', 'Tucson'] },
  { name: 'Jeep', models: ['Commander', 'Compass', 'Gladiator', 'Renegade'] },
  { name: 'Kia', models: ['Bongo', 'Cerato', 'Niro', 'Sportage', 'Stonic'] },
  { name: 'Mercedes-Benz', models: ['A 200', 'C 200', 'CLA 200', 'GLA 200', 'GLC 300', 'Sprinter'] },
  { name: 'Mitsubishi', models: ['Eclipse Cross', 'L200 Triton', 'Outlander', 'Pajero Sport'] },
  { name: 'Nissan', models: ['Frontier', 'Kicks', 'March', 'Sentra', 'Versa'] },
  { name: 'Peugeot', models: ['2008', '208', '3008', '308', 'Partner'] },
  { name: 'Ram', models: ['1500', '2500', 'Rampage'] },
  { name: 'Renault', models: ['Duster', 'Kardian', 'Kwid', 'Logan', 'Oroch', 'Sandero'] },
  { name: 'Toyota', models: ['Corolla', 'Corolla Cross', 'Hilux', 'RAV4', 'SW4', 'Yaris'] },
  { name: 'Volkswagen', models: ['Amarok', 'Gol', 'Nivus', 'Polo', 'Saveiro', 'Taos', 'T-Cross', 'Virtus', 'Voyage'] },
  { name: 'Volvo', models: ['C40', 'EX30', 'S60', 'XC40', 'XC60', 'XC90'] }
];

export const STANDARD_VEHICLE_COLORS = [
  'Amarelo', 'Azul', 'Bege', 'Branco', 'Cinza', 'Dourado', 'Laranja',
  'Marrom', 'Prata', 'Preto', 'Roxo', 'Verde', 'Vermelho', 'Vinho'
] as const;
