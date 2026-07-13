import os

files = [
    r'c:\src\mybill\Backend\src\main\java\com\mybill\MyBill_Backend\repository\ClientRepository.java',
    r'c:\src\mybill\Backend\src\main\java\com\mybill\MyBill_Backend\repository\ClientWorkRepository.java',
    r'c:\src\mybill\Backend\src\main\java\com\mybill\MyBill_Backend\repository\InvoiceRepository.java',
    r'c:\src\mybill\Backend\src\main\java\com\mybill\MyBill_Backend\repository\InvoiceItemRepository.java',
    r'c:\src\mybill\Backend\src\main\java\com\mybill\MyBill_Backend\repository\ClientLedgerEntryRepository.java'
]

methods = {
    'ClientRepository.java': '''
    @Query("""
           SELECT c FROM Client c
           WHERE c.user.id = :userId
             AND (c.updatedAt > :lastTime OR (c.updatedAt = :lastTime AND c.id > :lastId))
           """)
    org.springframework.data.domain.Page<com.mybill.MyBill_Backend.entity.Client> findByUserIdWithKeyset(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("lastTime") java.time.LocalDateTime lastTime,
            @org.springframework.data.repository.query.Param("lastId") java.util.UUID lastId,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT c FROM Client c WHERE c.user.id = :userId AND c.updatedAt >= :since")
    org.springframework.data.domain.Page<com.mybill.MyBill_Backend.entity.Client> findByUserIdAndUpdatedAtGreaterThanEqual(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since,
            org.springframework.data.domain.Pageable pageable
    );
''',
    'ClientWorkRepository.java': '''
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"client", "invoice"})
    @Query("""
           SELECT w FROM ClientWork w
           WHERE w.user.id = :userId
             AND (w.updatedAt > :lastTime OR (w.updatedAt = :lastTime AND w.id > :lastId))
           """)
    org.springframework.data.domain.Page<com.mybill.MyBill_Backend.entity.ClientWork> findByUserIdWithKeyset(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("lastTime") java.time.LocalDateTime lastTime,
            @org.springframework.data.repository.query.Param("lastId") java.util.UUID lastId,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"client", "invoice"})
    @Query("SELECT w FROM ClientWork w WHERE w.user.id = :userId AND w.updatedAt >= :since")
    org.springframework.data.domain.Page<com.mybill.MyBill_Backend.entity.ClientWork> findByUserIdAndUpdatedAtGreaterThanEqual(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since,
            org.springframework.data.domain.Pageable pageable
    );
''',
    'InvoiceRepository.java': '''
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"client"})
    @Query("""
           SELECT i FROM Invoice i
           WHERE i.user.id = :userId
             AND (i.updatedAt > :lastTime OR (i.updatedAt = :lastTime AND i.id > :lastId))
           """)
    org.springframework.data.domain.Page<com.mybill.MyBill_Backend.entity.Invoice> findByUserIdWithKeyset(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("lastTime") java.time.LocalDateTime lastTime,
            @org.springframework.data.repository.query.Param("lastId") java.util.UUID lastId,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"client"})
    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId AND i.updatedAt >= :since")
    org.springframework.data.domain.Page<com.mybill.MyBill_Backend.entity.Invoice> findByUserIdAndUpdatedAtGreaterThanEqual(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since,
            org.springframework.data.domain.Pageable pageable
    );
''',
    'InvoiceItemRepository.java': '''
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"invoice", "work"})
    @Query("""
           SELECT i FROM InvoiceItem i
           WHERE i.user.id = :userId
             AND (i.updatedAt > :lastTime OR (i.updatedAt = :lastTime AND i.id > :lastId))
           """)
    org.springframework.data.domain.Page<com.mybill.MyBill_Backend.entity.InvoiceItem> findByUserIdWithKeyset(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("lastTime") java.time.LocalDateTime lastTime,
            @org.springframework.data.repository.query.Param("lastId") java.util.UUID lastId,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"invoice", "work"})
    @Query("SELECT i FROM InvoiceItem i WHERE i.user.id = :userId AND i.updatedAt >= :since")
    org.springframework.data.domain.Page<com.mybill.MyBill_Backend.entity.InvoiceItem> findByUserIdAndUpdatedAtGreaterThanEqual(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since,
            org.springframework.data.domain.Pageable pageable
    );
''',
    'ClientLedgerEntryRepository.java': '''
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"client", "invoice", "payment"})
    @Query("""
           SELECT l FROM ClientLedgerEntry l
           WHERE l.user.id = :userId
             AND (l.updatedAt > :lastTime OR (l.updatedAt = :lastTime AND l.id > :lastId))
           """)
    org.springframework.data.domain.Page<com.mybill.MyBill_Backend.entity.ClientLedgerEntry> findByUserIdWithKeyset(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("lastTime") java.time.LocalDateTime lastTime,
            @org.springframework.data.repository.query.Param("lastId") java.util.UUID lastId,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"client", "invoice", "payment"})
    @Query("SELECT l FROM ClientLedgerEntry l WHERE l.user.id = :userId AND l.updatedAt >= :since")
    org.springframework.data.domain.Page<com.mybill.MyBill_Backend.entity.ClientLedgerEntry> findByUserIdAndUpdatedAtGreaterThanEqual(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since,
            org.springframework.data.domain.Pageable pageable
    );
'''
}

for filepath in files:
    filename = os.path.basename(filepath)
    if filename in methods:
        with open(filepath, 'r') as f:
            content = f.read()
        
        # Inject just before the final closing brace
        idx = content.rfind('}')
        if idx != -1:
            new_content = content[:idx] + methods[filename] + content[idx:]
            with open(filepath, 'w') as f:
                f.write(new_content)
            print(f'Patched {filename}')
        else:
            print(f'Failed to patch {filename}')
