// Code generated by protoc-gen-go. DO NOT EDIT.
// versions:
// 	protoc-gen-go v1.28.1
// 	protoc        v3.12.4
// source: sipc.proto

package sipc_proto

import (
	protoreflect "google.golang.org/protobuf/reflect/protoreflect"
	protoimpl "google.golang.org/protobuf/runtime/protoimpl"
	reflect "reflect"
	sync "sync"
)

const (
	// Verify that this generated code is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(20 - protoimpl.MinVersion)
	// Verify that runtime/protoimpl is sufficiently up-to-date.
	_ = protoimpl.EnforceVersion(protoimpl.MaxVersion - 20)
)

type TransportType int32

const (
	TransportType_kUnknown          TransportType = 0
	TransportType_kUnixDomainSocket TransportType = 1
	TransportType_kWindowsNamedPipe TransportType = 2
)

// Enum value maps for TransportType.
var (
	TransportType_name = map[int32]string{
		0: "kUnknown",
		1: "kUnixDomainSocket",
		2: "kWindowsNamedPipe",
	}
	TransportType_value = map[string]int32{
		"kUnknown":          0,
		"kUnixDomainSocket": 1,
		"kWindowsNamedPipe": 2,
	}
)

func (x TransportType) Enum() *TransportType {
	p := new(TransportType)
	*p = x
	return p
}

func (x TransportType) String() string {
	return protoimpl.X.EnumStringOf(x.Descriptor(), protoreflect.EnumNumber(x))
}

func (TransportType) Descriptor() protoreflect.EnumDescriptor {
	return file_sipc_proto_enumTypes[0].Descriptor()
}

func (TransportType) Type() protoreflect.EnumType {
	return &file_sipc_proto_enumTypes[0]
}

func (x TransportType) Number() protoreflect.EnumNumber {
	return protoreflect.EnumNumber(x)
}

// Deprecated: Use TransportType.Descriptor instead.
func (TransportType) EnumDescriptor() ([]byte, []int) {
	return file_sipc_proto_rawDescGZIP(), []int{0}
}

type ConnectInfo struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	ConnectionId     string        `protobuf:"bytes,1,opt,name=connection_id,json=connectionId,proto3" json:"connection_id,omitempty"`
	TransportType    TransportType `protobuf:"varint,2,opt,name=transport_type,json=transportType,proto3,enum=kr.jclab.sipc.TransportType" json:"transport_type,omitempty"`
	TransportAddress string        `protobuf:"bytes,3,opt,name=transport_address,json=transportAddress,proto3" json:"transport_address,omitempty"`
	PublicKey        []byte        `protobuf:"bytes,4,opt,name=public_key,json=publicKey,proto3" json:"public_key,omitempty"`
	AllowReconnect   bool          `protobuf:"varint,11,opt,name=allow_reconnect,json=allowReconnect,proto3" json:"allow_reconnect,omitempty"`
}

func (x *ConnectInfo) Reset() {
	*x = ConnectInfo{}
	if protoimpl.UnsafeEnabled {
		mi := &file_sipc_proto_msgTypes[0]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *ConnectInfo) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*ConnectInfo) ProtoMessage() {}

func (x *ConnectInfo) ProtoReflect() protoreflect.Message {
	mi := &file_sipc_proto_msgTypes[0]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use ConnectInfo.ProtoReflect.Descriptor instead.
func (*ConnectInfo) Descriptor() ([]byte, []int) {
	return file_sipc_proto_rawDescGZIP(), []int{0}
}

func (x *ConnectInfo) GetConnectionId() string {
	if x != nil {
		return x.ConnectionId
	}
	return ""
}

func (x *ConnectInfo) GetTransportType() TransportType {
	if x != nil {
		return x.TransportType
	}
	return TransportType_kUnknown
}

func (x *ConnectInfo) GetTransportAddress() string {
	if x != nil {
		return x.TransportAddress
	}
	return ""
}

func (x *ConnectInfo) GetPublicKey() []byte {
	if x != nil {
		return x.PublicKey
	}
	return nil
}

func (x *ConnectInfo) GetAllowReconnect() bool {
	if x != nil {
		return x.AllowReconnect
	}
	return false
}

type ClientHelloPayload struct {
	state         protoimpl.MessageState
	sizeCache     protoimpl.SizeCache
	unknownFields protoimpl.UnknownFields

	ConnectionId string `protobuf:"bytes,1,opt,name=connection_id,json=connectionId,proto3" json:"connection_id,omitempty"`
}

func (x *ClientHelloPayload) Reset() {
	*x = ClientHelloPayload{}
	if protoimpl.UnsafeEnabled {
		mi := &file_sipc_proto_msgTypes[1]
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		ms.StoreMessageInfo(mi)
	}
}

func (x *ClientHelloPayload) String() string {
	return protoimpl.X.MessageStringOf(x)
}

func (*ClientHelloPayload) ProtoMessage() {}

func (x *ClientHelloPayload) ProtoReflect() protoreflect.Message {
	mi := &file_sipc_proto_msgTypes[1]
	if protoimpl.UnsafeEnabled && x != nil {
		ms := protoimpl.X.MessageStateOf(protoimpl.Pointer(x))
		if ms.LoadMessageInfo() == nil {
			ms.StoreMessageInfo(mi)
		}
		return ms
	}
	return mi.MessageOf(x)
}

// Deprecated: Use ClientHelloPayload.ProtoReflect.Descriptor instead.
func (*ClientHelloPayload) Descriptor() ([]byte, []int) {
	return file_sipc_proto_rawDescGZIP(), []int{1}
}

func (x *ClientHelloPayload) GetConnectionId() string {
	if x != nil {
		return x.ConnectionId
	}
	return ""
}

var File_sipc_proto protoreflect.FileDescriptor

var file_sipc_proto_rawDesc = []byte{
	0x0a, 0x0a, 0x73, 0x69, 0x70, 0x63, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x12, 0x0d, 0x6b, 0x72,
	0x2e, 0x6a, 0x63, 0x6c, 0x61, 0x62, 0x2e, 0x73, 0x69, 0x70, 0x63, 0x22, 0xec, 0x01, 0x0a, 0x0b,
	0x43, 0x6f, 0x6e, 0x6e, 0x65, 0x63, 0x74, 0x49, 0x6e, 0x66, 0x6f, 0x12, 0x23, 0x0a, 0x0d, 0x63,
	0x6f, 0x6e, 0x6e, 0x65, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x5f, 0x69, 0x64, 0x18, 0x01, 0x20, 0x01,
	0x28, 0x09, 0x52, 0x0c, 0x63, 0x6f, 0x6e, 0x6e, 0x65, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x49, 0x64,
	0x12, 0x43, 0x0a, 0x0e, 0x74, 0x72, 0x61, 0x6e, 0x73, 0x70, 0x6f, 0x72, 0x74, 0x5f, 0x74, 0x79,
	0x70, 0x65, 0x18, 0x02, 0x20, 0x01, 0x28, 0x0e, 0x32, 0x1c, 0x2e, 0x6b, 0x72, 0x2e, 0x6a, 0x63,
	0x6c, 0x61, 0x62, 0x2e, 0x73, 0x69, 0x70, 0x63, 0x2e, 0x54, 0x72, 0x61, 0x6e, 0x73, 0x70, 0x6f,
	0x72, 0x74, 0x54, 0x79, 0x70, 0x65, 0x52, 0x0d, 0x74, 0x72, 0x61, 0x6e, 0x73, 0x70, 0x6f, 0x72,
	0x74, 0x54, 0x79, 0x70, 0x65, 0x12, 0x2b, 0x0a, 0x11, 0x74, 0x72, 0x61, 0x6e, 0x73, 0x70, 0x6f,
	0x72, 0x74, 0x5f, 0x61, 0x64, 0x64, 0x72, 0x65, 0x73, 0x73, 0x18, 0x03, 0x20, 0x01, 0x28, 0x09,
	0x52, 0x10, 0x74, 0x72, 0x61, 0x6e, 0x73, 0x70, 0x6f, 0x72, 0x74, 0x41, 0x64, 0x64, 0x72, 0x65,
	0x73, 0x73, 0x12, 0x1d, 0x0a, 0x0a, 0x70, 0x75, 0x62, 0x6c, 0x69, 0x63, 0x5f, 0x6b, 0x65, 0x79,
	0x18, 0x04, 0x20, 0x01, 0x28, 0x0c, 0x52, 0x09, 0x70, 0x75, 0x62, 0x6c, 0x69, 0x63, 0x4b, 0x65,
	0x79, 0x12, 0x27, 0x0a, 0x0f, 0x61, 0x6c, 0x6c, 0x6f, 0x77, 0x5f, 0x72, 0x65, 0x63, 0x6f, 0x6e,
	0x6e, 0x65, 0x63, 0x74, 0x18, 0x0b, 0x20, 0x01, 0x28, 0x08, 0x52, 0x0e, 0x61, 0x6c, 0x6c, 0x6f,
	0x77, 0x52, 0x65, 0x63, 0x6f, 0x6e, 0x6e, 0x65, 0x63, 0x74, 0x22, 0x39, 0x0a, 0x12, 0x43, 0x6c,
	0x69, 0x65, 0x6e, 0x74, 0x48, 0x65, 0x6c, 0x6c, 0x6f, 0x50, 0x61, 0x79, 0x6c, 0x6f, 0x61, 0x64,
	0x12, 0x23, 0x0a, 0x0d, 0x63, 0x6f, 0x6e, 0x6e, 0x65, 0x63, 0x74, 0x69, 0x6f, 0x6e, 0x5f, 0x69,
	0x64, 0x18, 0x01, 0x20, 0x01, 0x28, 0x09, 0x52, 0x0c, 0x63, 0x6f, 0x6e, 0x6e, 0x65, 0x63, 0x74,
	0x69, 0x6f, 0x6e, 0x49, 0x64, 0x2a, 0x4b, 0x0a, 0x0d, 0x54, 0x72, 0x61, 0x6e, 0x73, 0x70, 0x6f,
	0x72, 0x74, 0x54, 0x79, 0x70, 0x65, 0x12, 0x0c, 0x0a, 0x08, 0x6b, 0x55, 0x6e, 0x6b, 0x6e, 0x6f,
	0x77, 0x6e, 0x10, 0x00, 0x12, 0x15, 0x0a, 0x11, 0x6b, 0x55, 0x6e, 0x69, 0x78, 0x44, 0x6f, 0x6d,
	0x61, 0x69, 0x6e, 0x53, 0x6f, 0x63, 0x6b, 0x65, 0x74, 0x10, 0x01, 0x12, 0x15, 0x0a, 0x11, 0x6b,
	0x57, 0x69, 0x6e, 0x64, 0x6f, 0x77, 0x73, 0x4e, 0x61, 0x6d, 0x65, 0x64, 0x50, 0x69, 0x70, 0x65,
	0x10, 0x02, 0x42, 0x46, 0x0a, 0x13, 0x6b, 0x72, 0x2e, 0x6a, 0x63, 0x6c, 0x61, 0x62, 0x2e, 0x73,
	0x69, 0x70, 0x63, 0x2e, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x42, 0x09, 0x53, 0x69, 0x70, 0x63, 0x50,
	0x72, 0x6f, 0x74, 0x6f, 0x5a, 0x24, 0x67, 0x69, 0x74, 0x68, 0x75, 0x62, 0x2e, 0x63, 0x6f, 0x6d,
	0x2f, 0x6a, 0x63, 0x2d, 0x6c, 0x61, 0x62, 0x2f, 0x73, 0x69, 0x70, 0x63, 0x2f, 0x67, 0x6f, 0x2f,
	0x73, 0x69, 0x70, 0x63, 0x5f, 0x70, 0x72, 0x6f, 0x74, 0x6f, 0x62, 0x06, 0x70, 0x72, 0x6f, 0x74,
	0x6f, 0x33,
}

var (
	file_sipc_proto_rawDescOnce sync.Once
	file_sipc_proto_rawDescData = file_sipc_proto_rawDesc
)

func file_sipc_proto_rawDescGZIP() []byte {
	file_sipc_proto_rawDescOnce.Do(func() {
		file_sipc_proto_rawDescData = protoimpl.X.CompressGZIP(file_sipc_proto_rawDescData)
	})
	return file_sipc_proto_rawDescData
}

var file_sipc_proto_enumTypes = make([]protoimpl.EnumInfo, 1)
var file_sipc_proto_msgTypes = make([]protoimpl.MessageInfo, 2)
var file_sipc_proto_goTypes = []interface{}{
	(TransportType)(0),         // 0: kr.jclab.sipc.TransportType
	(*ConnectInfo)(nil),        // 1: kr.jclab.sipc.ConnectInfo
	(*ClientHelloPayload)(nil), // 2: kr.jclab.sipc.ClientHelloPayload
}
var file_sipc_proto_depIdxs = []int32{
	0, // 0: kr.jclab.sipc.ConnectInfo.transport_type:type_name -> kr.jclab.sipc.TransportType
	1, // [1:1] is the sub-list for method output_type
	1, // [1:1] is the sub-list for method input_type
	1, // [1:1] is the sub-list for extension type_name
	1, // [1:1] is the sub-list for extension extendee
	0, // [0:1] is the sub-list for field type_name
}

func init() { file_sipc_proto_init() }
func file_sipc_proto_init() {
	if File_sipc_proto != nil {
		return
	}
	if !protoimpl.UnsafeEnabled {
		file_sipc_proto_msgTypes[0].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*ConnectInfo); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
		file_sipc_proto_msgTypes[1].Exporter = func(v interface{}, i int) interface{} {
			switch v := v.(*ClientHelloPayload); i {
			case 0:
				return &v.state
			case 1:
				return &v.sizeCache
			case 2:
				return &v.unknownFields
			default:
				return nil
			}
		}
	}
	type x struct{}
	out := protoimpl.TypeBuilder{
		File: protoimpl.DescBuilder{
			GoPackagePath: reflect.TypeOf(x{}).PkgPath(),
			RawDescriptor: file_sipc_proto_rawDesc,
			NumEnums:      1,
			NumMessages:   2,
			NumExtensions: 0,
			NumServices:   0,
		},
		GoTypes:           file_sipc_proto_goTypes,
		DependencyIndexes: file_sipc_proto_depIdxs,
		EnumInfos:         file_sipc_proto_enumTypes,
		MessageInfos:      file_sipc_proto_msgTypes,
	}.Build()
	File_sipc_proto = out.File
	file_sipc_proto_rawDesc = nil
	file_sipc_proto_goTypes = nil
	file_sipc_proto_depIdxs = nil
}
