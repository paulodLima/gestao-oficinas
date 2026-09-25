package br.com.gestao.oficinas_api.ordem;

import br.com.gestao.oficinas_api.identidade.*;
import java.util.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/ordens-servico/{orderId}/fotos")
public class ServicePhotoController {
    private final ServiceOrderService orders; private final ServicePhotoRepository photos; private final PhotoStorage storage;
    public ServicePhotoController(ServiceOrderService orders, ServicePhotoRepository photos, PhotoStorage storage) { this.orders=orders; this.photos=photos; this.storage=storage; }
    @GetMapping public List<ServicePhoto> list(Authentication auth,@PathVariable UUID orderId) { Identidade owner=identity(auth); orders.order(owner,orderId); return photos.list(owner.oficinaId(),orderId); }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @Transactional public ResponseEntity<ServicePhoto> upload(Authentication auth,@PathVariable UUID orderId,@RequestPart MultipartFile arquivo,@RequestParam UUID uploadId,@RequestParam ServiceOrderStatus etapa,@RequestParam(required=false) String legenda,@RequestParam(defaultValue="false") boolean publicada) {
        Identidade owner=identity(auth); orders.lockActive(owner,orderId);
        var replay=photos.byUpload(owner.oficinaId(),orderId,uploadId); if(replay.isPresent()) return ResponseEntity.ok(replay.get());
        if(legenda!=null && legenda.length()>500) throw new ApiException(400,"DADOS_INVALIDOS","A legenda deve ter até 500 caracteres.");
        PhotoStorage.StoredPhoto stored=storage.store(arquivo); try { photos.add(owner.oficinaId(),owner.id(),orderId,uploadId,etapa,blank(legenda),publicada,arquivo.getOriginalFilename()==null?"foto":arquivo.getOriginalFilename(),stored); }
        catch(RuntimeException e) { storage.delete(stored.key()); storage.delete(stored.thumbnailKey()); throw e; }
        return ResponseEntity.status(HttpStatus.CREATED).body(photos.byUpload(owner.oficinaId(),orderId,uploadId).orElseThrow());
    }
    @GetMapping("/{photoId}/arquivo") public ResponseEntity<ByteArrayResource> file(Authentication auth,@PathVariable UUID orderId,@PathVariable UUID photoId,@RequestParam(defaultValue="false") boolean miniatura) {
        Identidade owner=identity(auth); orders.order(owner,orderId);
        var found=photos.find(owner.oficinaId(),orderId,photoId);
        String key=miniatura && found.thumbnailKey()!=null?found.thumbnailKey():found.key();
        var display=storage.readDisplay(key);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType(display.contentType()))
            .body(new ByteArrayResource(display.bytes()));
    }
    @DeleteMapping("/{photoId}") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void remove(Authentication auth,@PathVariable UUID orderId,@PathVariable UUID photoId) { Identidade owner=identity(auth); orders.lockActive(owner,orderId); var found=photos.find(owner.oficinaId(),orderId,photoId); photos.remove(owner.oficinaId(),owner.id(),orderId,photoId); storage.delete(found.key()); storage.delete(found.thumbnailKey()); }
    private static String blank(String value) { return value==null||value.isBlank()?null:value.trim(); }
    private Identidade identity(Authentication auth) { if(auth==null || !(auth.getDetails() instanceof Identidade owner)) throw new ApiException(401,"NAO_AUTENTICADO","Entre novamente."); return owner; }
}
