all: dice 

#CFLAGS += -ffunction-sections
#LDFLAGS += -Wl,--gc-sections,--undefined=_reset_vector__,--undefined=InterruptVectors,--undefined=_copy_data_init__,--undefined=_clear_bss_init__,--undefined=_end_of_init__
#CFLAGS += -DROCESS_CONF_NO_PROCESS_NAMES=0
#CFLAGS += -DUIP_CONF_UDP=0
#CFLAGS += -DCONTIKI_NO_NET=1
#CFLAGS += -DMAC_CONF_DRIVER=nullmac_driver

CONTIKI = ../..
include $(CONTIKI)/Makefile.include

dice.$(TARGET): $(OBJECTDIR)/drickle.o $(OBJECTDIR)/view_manager.o $(OBJECTDIR)/attributes.o $(OBJECTDIR)/dummy_group.o $(OBJECTDIR)/history.o

