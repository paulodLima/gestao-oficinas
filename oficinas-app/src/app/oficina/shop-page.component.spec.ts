import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ShopPageComponent } from './shop-page.component';
import { ShopService, ShopProfile } from './shop.service';

describe('Configuração da oficina', () => {
  const profile: ShopProfile = { id: 'id', slug: 'oficina-teste', nome: 'Oficina Teste', telefone: '',
    emailContato: '', endereco: '', horario: '', fuso: 'America/Sao_Paulo', perfilPublico: false, temLogo: false, versao: 0 };
  let service: jasmine.SpyObj<ShopService>;
  beforeEach(() => {
    service = jasmine.createSpyObj<ShopService>('ShopService', ['get', 'save', 'upload', 'remove']);
    service.get.and.resolveTo(profile);
    TestBed.configureTestingModule({ imports: [ShopPageComponent], providers: [provideRouter([]), { provide: ShopService, useValue: service }] });
  });
  it('carrega dados e bloqueia nome em branco e e-mail inválido', async () => {
    const component = TestBed.createComponent(ShopPageComponent).componentInstance;
    await component.load();
    expect(component.form.controls.nome.value).toBe('Oficina Teste');
    component.form.controls.nome.setValue(' ');
    await component.save();
    expect(service.save).not.toHaveBeenCalled();
    component.form.controls.nome.setValue('Teste');
    component.form.controls.emailContato.setValue('inválido');
    await component.save();
    expect(service.save).not.toHaveBeenCalled();
  });
  it('preserva os campos e a versão quando o salvamento falha', async () => {
    const component = TestBed.createComponent(ShopPageComponent).componentInstance;
    await component.load();
    component.form.controls.nome.setValue('Nova oficina');
    service.save.and.rejectWith(new Error('offline'));
    await component.save();
    expect(component.form.controls.nome.value).toBe('Nova oficina');
    expect(component.shop()?.versao).toBe(0);
    expect(component.error()).toBeTruthy();
    expect(component.success()).toBe('');
    expect(component.busy()).toBeFalse();
  });
  it('envia a versão e confirma somente depois da resposta', async () => {
    const component = TestBed.createComponent(ShopPageComponent).componentInstance;
    await component.load();
    service.save.and.resolveTo({ ...profile, nome: 'Oficina Nova', versao: 1 });
    component.form.controls.nome.setValue('Oficina Nova');
    await component.save();
    expect(service.save).toHaveBeenCalledWith(jasmine.objectContaining({ nome: 'Oficina Nova', versao: 0 }));
    expect(component.shop()?.versao).toBe(1);
    expect(component.success()).toBeTruthy();
  });
  it('remove sucesso anterior em validação inválida ou recarga com erro', async () => {
    const component = TestBed.createComponent(ShopPageComponent).componentInstance;
    await component.load();
    component.success.set('Alteração salva.');
    component.form.controls.nome.setValue(' ');
    await component.save();
    expect(component.success()).toBe('');
    component.success.set('Alteração salva.');
    service.get.and.rejectWith(new Error('offline'));
    await component.load();
    expect(component.success()).toBe('');
    expect(component.error()).toBeTruthy();
  });
});
