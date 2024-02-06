
package org.hotswap.agent.plugin.hibernate_jakarta;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.Transient;

import org.hibernate.Version;
import org.hotswap.agent.plugin.hibernate_jakarta.testEntities.TestEntity;
import org.hotswap.agent.plugin.hibernate_jakarta.testEntitiesHotswap.TestEntity2;
import org.hotswap.agent.plugin.hotswapper.HotSwapper;
import org.hotswap.agent.util.test.WaitHelper;
import org.junit.BeforeClass;
import org.junit.Test;


public class HibernateJakartaPluginTest {

    static EntityManagerFactory entityManagerFactory;

    @BeforeClass
    public static void setup() throws Exception {
        String[] version = Version.getVersionString().split("\\.");
        boolean version52OrGreater = Integer.valueOf(version[0]) == 5 && Integer.valueOf(version[1]) >= 2;
        if (version52OrGreater) {
            entityManagerFactory = Persistence.createEntityManagerFactory("TestPU52");
        }  else {
            entityManagerFactory = Persistence.createEntityManagerFactory("TestPU");
        }
    }

    @Test
    public void testSetupOk() throws Exception {
        doInTransaction(new InTransaction() {
            @Override
            public void process(EntityManager entityManager) throws Exception {
                TestEntity entity = (TestEntity) entityManager.createQuery(
                        "from TestEntity where name='Test'").getSingleResult();

                assertNotNull(entity);
                assertEquals("Test", entity.getName());
                assertEquals("descr", entity.getDescription());
            }
        });

        swapClasses();

        doInTransaction(new InTransaction() {
            @Override
            public void process(EntityManager entityManager) throws Exception {
                TestEntity entity = (TestEntity) entityManager.createQuery(
                        "from TestEntity where name='Test'").getSingleResult();

                assertNotNull(entity);
                assertEquals("Test", entity.getName());
                assertNull(entity.getDescription());
            }
        });
    }

    private void swapClasses() throws Exception {


        assertTrue(TestEntity.class.getDeclaredField("description").getAnnotations().length == 0);

        HibernateRefreshCommands.reloadFlag = true;
        HotSwapper.swapClasses(TestEntity.class, TestEntity2.class.getName());
        assertTrue(WaitHelper.waitForCommand(new WaitHelper.Command() {
            @Override
            public boolean result() throws Exception {
                return !HibernateRefreshCommands.reloadFlag;
            }
        }));


        assertTrue(TestEntity.class.getDeclaredField("description").getAnnotation(Transient.class) != null);
    }


    private void doInTransaction(InTransaction inTransaction) throws Exception {
        System.out.println(entityManagerFactory);
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        try {
            TestEntity simpleEntity = new TestEntity("Test", "descr");
            entityManager.persist(simpleEntity);


            entityManager.flush();
            entityManager.clear();

            inTransaction.process(entityManager);
        } finally {
            entityManager.getTransaction().rollback();
            entityManager.close();
        }
    }

    private static interface InTransaction {
        public void process(EntityManager entityManager) throws Exception;
    }
}
